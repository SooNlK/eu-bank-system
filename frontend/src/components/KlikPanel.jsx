import { useState, useEffect, useRef } from 'react'
import { getMyAccounts } from '../services/account'
import {
    generateKlikCode,
    getKlikPendingTransactions,
    confirmKlikTransaction,
    registerKlikAlias,
    unregisterKlikAlias,
    createKlikP2pTransfer
} from '../services/klik'

const inputClass =
    'w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400'

const labelClass = 'text-[11px] text-slate-500 font-medium mb-1.5 block'

function formatEur(amount, currency = 'EUR') {
    return new Intl.NumberFormat('pl-PL', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
    }).format(amount)
}

export default function KlikPanel({ userEmail }) {
    const [accounts, setAccounts] = useState([])
    const [selectedAccount, setSelectedAccount] = useState('')
    const [activeTab, setActiveTab] = useState('c2b')
    const [loading, setLoading] = useState(false)
    const [message, setMessage] = useState(null)

    // C2B States
    const [klikCode, setKlikCode] = useState('')
    const [timeLeft, setTimeLeft] = useState(0)
    const [pendingTx, setPendingTx] = useState(null)
    const timerRef = useRef(null)
    const pollRef = useRef(null)

    // P2P States
    const [p2pPhone, setP2pPhone] = useState('')
    const [p2pAmount, setP2pAmount] = useState('')
    const [p2pDescription, setP2pDescription] = useState('Przelew na telefon KLIK')

    // Alias States
    const [regPhone, setRegPhone] = useState('')
    const [registeredAliases, setRegisteredAliases] = useState(() => {
        try {
            const storageKey = userEmail ? `klik_registered_aliases_${userEmail}` : 'klik_registered_aliases'
            return JSON.parse(localStorage.getItem(storageKey) || '[]')
        } catch {
            return []
        }
    })

    useEffect(() => {
        async function loadAccounts() {
            try {
                const accs = await getMyAccounts()
                setAccounts(accs)
                if (accs.length > 0) setSelectedAccount(accs[0].id)
            } catch (err) {
                showMessage(err.message, 'error')
            }
        }
        loadAccounts()
        return () => {
            clearInterval(timerRef.current)
            clearInterval(pollRef.current)
        }
    }, [])

    useEffect(() => {
        const storageKey = userEmail ? `klik_registered_aliases_${userEmail}` : 'klik_registered_aliases'
        localStorage.setItem(storageKey, JSON.stringify(registeredAliases))
    }, [registeredAliases, userEmail])

    // Convert any phone number to E.164 format (+49XXXXXXXXXX for Germany/EU zone)
    function toE164(raw) {
        let num = raw.replace(/[\s\-()]/g, '')
        if (/^\+\d{7,15}$/.test(num)) return num
        if (/^00\d{7,15}$/.test(num)) return '+' + num.slice(2)
        if (/^0\d{5,14}$/.test(num)) return '+49' + num.slice(1)
        if (/^\d{5,14}$/.test(num)) return '+49' + num
        return num
    }

    function showMessage(text, type = 'success') {
        setMessage({ text, type })
        setTimeout(() => setMessage(null), 5000)
    }

    // --- C2B ---

    async function handleGenerateCode() {
        if (!selectedAccount) { showMessage('Wybierz rachunek', 'error'); return }
        setLoading(true)
        try {
            const res = await generateKlikCode(selectedAccount)
            setKlikCode(res.code)
            setTimeLeft(120)
            clearInterval(timerRef.current)
            timerRef.current = setInterval(() => {
                setTimeLeft(prev => {
                    if (prev <= 1) {
                        clearInterval(timerRef.current)
                        clearInterval(pollRef.current)
                        setKlikCode('')
                        return 0
                    }
                    return prev - 1
                })
            }, 1000)
            startPollingPending()
        } catch (err) {
            showMessage(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    function startPollingPending() {
        clearInterval(pollRef.current)
        pollRef.current = setInterval(async () => {
            try {
                const pending = await getKlikPendingTransactions()
                if (pending && pending.length > 0) {
                    setPendingTx(pending[0])
                    clearInterval(pollRef.current)
                }
            } catch (err) {
                console.error('Błąd pobierania transakcji oczekujących:', err)
            }
        }, 2000)
    }

    async function handleConfirmTransaction(status) {
        if (!pendingTx) return
        setLoading(true)
        try {
            await confirmKlikTransaction(pendingTx.transactionId, status)
            showMessage(
                status === 'ACCEPTED'
                    ? 'Płatność zaakceptowana. Środki zostały zaksięgowane.'
                    : 'Płatność została odrzucona.',
                status === 'ACCEPTED' ? 'success' : 'error'
            )
            setPendingTx(null)
            setKlikCode('')
            setTimeLeft(0)
            clearInterval(timerRef.current)
        } catch (err) {
            showMessage(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    // --- P2P ---

    async function handleP2pTransfer(e) {
        e.preventDefault()
        if (!selectedAccount || !p2pPhone || !p2pAmount) {
            showMessage('Wypełnij wszystkie pola formularza', 'error')
            return
        }
        setLoading(true)
        try {
            const formattedPhone = toE164(p2pPhone)
            const res = await createKlikP2pTransfer({
                fromAccountId: selectedAccount,
                toPhone: formattedPhone,
                amount: parseFloat(p2pAmount),
                currency: 'EUR',
                description: p2pDescription
            })
            showMessage(`Przelew zrealizowany pomyślnie. ID: ${res.id}`)
            setP2pPhone('')
            setP2pAmount('')
            setP2pDescription('Przelew na telefon KLIK')
        } catch (err) {
            showMessage(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    // --- Aliases ---

    async function handleRegisterAlias(e) {
        e.preventDefault()
        if (!selectedAccount || !regPhone) { showMessage('Podaj numer telefonu', 'error'); return }
        setLoading(true)
        try {
            const formattedPhone = toE164(regPhone)
            const res = await registerKlikAlias(selectedAccount, formattedPhone)
            const acc = accounts.find(a => a.id === selectedAccount)
            const newAlias = {
                phone: formattedPhone,
                accountId: selectedAccount,
                iban: acc ? acc.accountNumber : 'N/A',
                registeredAt: res.registered_at || new Date().toISOString()
            }
            setRegisteredAliases(prev => [newAlias, ...prev.filter(a => a.phone !== formattedPhone)])
            showMessage(`Numer ${formattedPhone} został zarejestrowany.`)
            setRegPhone('')
        } catch (err) {
            showMessage(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    async function handleUnregisterAlias(phone) {
        setLoading(true)
        try {
            await unregisterKlikAlias(phone)
            setRegisteredAliases(prev => prev.filter(a => a.phone !== phone))
            showMessage(`Numer ${phone} został wyrejestrowany.`)
        } catch (err) {
            showMessage(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    const selectedAcc = accounts.find(a => a.id === selectedAccount)

    return (
        <div className="flex flex-col gap-4">
            {/* Page header */}
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-[22px] font-bold text-slate-900">
                        Płatności KLIK
                    </h1>
                    <p className="text-[12px] text-slate-500 mt-1">
                        Generuj kody płatności lub wysyłaj przelewy błyskawiczne na numer telefonu.
                    </p>
                </div>
            </div>

            {/* Inline message bar */}
            {message && (
                <div className={`rounded-lg border p-4 flex items-start gap-3 shadow-sm transition-all duration-300 ${
                    message.type === 'error'
                        ? 'bg-rose-50/90 border-rose-200/70 text-rose-900'
                        : 'bg-emerald-50/90 border-emerald-200/70 text-emerald-900'
                }`}>
                    {message.type === 'error' ? (
                        <svg className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                        </svg>
                    ) : (
                        <svg className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    )}
                    <div className="flex-1 text-[13px] font-medium leading-relaxed">{message.text}</div>
                    <button onClick={() => setMessage(null)} className="text-slate-400 hover:text-slate-600 border-none bg-transparent cursor-pointer leading-none text-[16px] px-1 focus:outline-none">×</button>
                </div>
            )}

            <div className="grid grid-cols-[300px_1fr] gap-4 items-start">
                {/* Left sidebar: account picker + tab nav */}
                <div className="flex flex-col gap-4">
                    {/* Account selector card */}
                    <div className="bg-white rounded-2xl border border-slate-200/70 p-4 flex flex-col gap-3">
                        <h2 className="text-[15px] font-semibold text-slate-900">Rachunek</h2>
                        <label className="flex flex-col gap-1.5">
                            <span className={labelClass}>Aktywny rachunek</span>
                            <select
                                value={selectedAccount}
                                onChange={e => setSelectedAccount(e.target.value)}
                                className="h-9 rounded-lg border border-slate-200 px-3 text-[12px] bg-slate-50 text-slate-800 outline-none focus:border-blue-400"
                            >
                                {accounts.map(acc => (
                                    <option key={acc.id} value={acc.id}>
                                        {acc.type === 'JUNIOR' ? '🧸 ' : ''}{acc.accountNumber}
                                    </option>
                                ))}
                            </select>
                        </label>
                        {selectedAcc && (
                            <div className="rounded-lg bg-slate-50 px-3 py-2 text-[11px] text-slate-500">
                                Saldo:{' '}
                                <span className="font-semibold text-slate-700">
                                    {formatEur(selectedAcc.balance, selectedAcc.currency)}
                                </span>
                            </div>
                        )}
                    </div>

                    {/* Tab navigation */}
                    <div className="bg-white rounded-2xl border border-slate-200/70 p-1.5 flex flex-col gap-0.5">
                        {[
                            { id: 'c2b', label: 'Kod KLIK (Płatność)', icon: (
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                                    <rect x="5" y="2" width="14" height="20" rx="2" stroke="currentColor" strokeWidth="1.8"/>
                                    <path d="M9 18h6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
                                    <path d="M9 7h6M9 11h4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
                                </svg>
                            )},
                            { id: 'p2p', label: 'Przelew na telefon', icon: (
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                                    <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
                                </svg>
                            )},
                            { id: 'aliases', label: 'Zarządzaj aliasami', icon: (
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                                    <circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="1.8"/>
                                    <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
                                </svg>
                            )},
                        ].map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`flex items-center gap-2.5 w-full text-left px-3 py-2.5 rounded-[8px] border-none text-[12px] font-medium cursor-pointer transition-all duration-150 ${
                                    activeTab === tab.id
                                        ? 'bg-blue-50 text-blue-700'
                                        : 'bg-transparent text-slate-500 hover:bg-slate-50 hover:text-slate-700'
                                }`}
                            >
                                <span className={activeTab === tab.id ? 'text-blue-600' : 'text-slate-400'}>{tab.icon}</span>
                                {tab.label}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Right content area */}
                <div className="bg-white rounded-2xl border border-slate-200/70 p-5 min-h-[380px] flex flex-col">

                    {/* ── C2B Tab ── */}
                    {activeTab === 'c2b' && (
                        <div className="flex flex-col flex-1">
                            <h2 className="text-[15px] font-semibold text-slate-900 mb-4">Generowanie kodu płatności</h2>
                            {klikCode ? (
                                <div className="flex flex-col items-center gap-6 flex-1 justify-center">
                                    <p className={labelClass + ' uppercase tracking-widest'}>Twój jednorazowy kod KLIK</p>
                                    <div className="bg-slate-50 border border-slate-200 rounded-lg px-10 py-6 flex items-center justify-center">
                                        <span className="text-5xl font-mono font-bold tracking-[0.22em] text-slate-800 pl-[0.22em] select-all">
                                            {klikCode}
                                        </span>
                                    </div>
                                    <div className="flex flex-col items-center gap-2 w-full max-w-xs">
                                        <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
                                            <div
                                                className={`h-full transition-all duration-1000 ease-linear rounded-full ${timeLeft > 40 ? 'bg-emerald-500' : timeLeft > 15 ? 'bg-amber-500' : 'bg-rose-500'}`}
                                                style={{ width: `${(timeLeft / 120) * 100}%` }}
                                            />
                                        </div>
                                        <p className="text-[12px] text-slate-500">
                                            Kod wygaśnie za{' '}
                                            <span className="font-semibold text-slate-800">{timeLeft} s</span>
                                        </p>
                                    </div>
                                    <p className="text-[11px] text-slate-400 text-center max-w-sm">
                                        Wpisz kod w terminalu płatniczym lub u agenta i zatwierdź transakcję.
                                    </p>
                                </div>
                            ) : (
                                <div className="flex flex-col items-center justify-center gap-5 flex-1 text-center">
                                    <div className="w-14 h-14 bg-slate-100 rounded-lg flex items-center justify-center">
                                        <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
                                            <rect x="5" y="2" width="14" height="20" rx="2" stroke="#64748b" strokeWidth="1.6"/>
                                            <path d="M9 18h6" stroke="#64748b" strokeWidth="1.6" strokeLinecap="round"/>
                                            <path d="M9 7h6M9 11h4" stroke="#64748b" strokeWidth="1.6" strokeLinecap="round"/>
                                        </svg>
                                    </div>
                                    <div>
                                        <p className="text-[14px] font-semibold text-slate-800">Generowanie płatności kodem</p>
                                        <p className="text-[12px] text-slate-500 mt-1 max-w-xs">
                                            Generuje jednorazowy 6-cyfrowy kod KLIK do płatności w sklepie internetowym lub stacjonarnym.
                                        </p>
                                    </div>
                                    <button
                                        onClick={handleGenerateCode}
                                        disabled={loading || !selectedAccount}
                                        className="h-10 px-6 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-[13px] border-none rounded-lg cursor-pointer transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {loading ? 'Generowanie...' : 'Generuj kod KLIK'}
                                    </button>
                                </div>
                            )}
                        </div>
                    )}

                    {/* ── P2P Tab ── */}
                    {activeTab === 'p2p' && (
                        <form onSubmit={handleP2pTransfer} className="flex flex-col gap-4">
                            <h2 className="text-[15px] font-semibold text-slate-900">Przelew na telefon</h2>

                            <label className="flex flex-col">
                                <span className={labelClass}>Numer telefonu odbiorcy</span>
                                <input
                                    type="tel"
                                    placeholder="np. +49 151 12345678"
                                    value={p2pPhone}
                                    onChange={e => setP2pPhone(e.target.value)}
                                    className={inputClass}
                                    required
                                />
                                <span className="text-[10px] text-slate-400 mt-1">
                                    Prefiks +49 zostanie dodany automatycznie dla numerów bez kodu kraju.
                                </span>
                            </label>

                            <div className="grid grid-cols-2 gap-3">
                                <label className="flex flex-col">
                                    <span className={labelClass}>Kwota</span>
                                    <input
                                        type="number"
                                        step="0.01"
                                        min="0.01"
                                        placeholder="0.00"
                                        value={p2pAmount}
                                        onChange={e => setP2pAmount(e.target.value)}
                                        className={inputClass}
                                        required
                                    />
                                </label>
                                <label className="flex flex-col">
                                    <span className={labelClass}>Waluta</span>
                                    <input
                                        type="text"
                                        value="EUR"
                                        className={inputClass + ' opacity-60 cursor-not-allowed'}
                                        disabled
                                    />
                                </label>
                            </div>

                            <label className="flex flex-col">
                                <span className={labelClass}>Tytuł przelewu</span>
                                <input
                                    type="text"
                                    value={p2pDescription}
                                    onChange={e => setP2pDescription(e.target.value)}
                                    className={inputClass}
                                    required
                                />
                            </label>

                            <div className="rounded-lg bg-slate-50 px-3 py-2 text-[11px] text-slate-500">
                                Rachunek obciążony:{' '}
                                <span className="font-semibold text-slate-700 font-mono">
                                    {selectedAcc?.accountNumber || '—'}
                                </span>
                            </div>

                            <button
                                type="submit"
                                disabled={loading || !selectedAccount}
                                className="h-10 px-6 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-[13px] border-none rounded-lg cursor-pointer transition-colors disabled:opacity-50 disabled:cursor-not-allowed self-start"
                            >
                                {loading ? 'Przetwarzanie...' : 'Wyślij przelew'}
                            </button>
                        </form>
                    )}

                    {/* ── Aliases Tab ── */}
                    {activeTab === 'aliases' && (
                        <div className="flex flex-col gap-5">
                            <h2 className="text-[15px] font-semibold text-slate-900">Zarządzaj aliasami</h2>

                            {/* Registration form */}
                            <form onSubmit={handleRegisterAlias} className="flex flex-col gap-3 bg-slate-50 rounded-lg border border-slate-200 p-4">
                                <div>
                                    <p className="text-[13px] font-semibold text-slate-800">Zarejestruj nowy numer</p>
                                    <p className="text-[11px] text-slate-500 mt-0.5">
                                        Powiązanie numeru telefonu z rachunkiem umożliwia innym użytkownikom wysyłanie Ci przelewów bez podawania IBAN.
                                    </p>
                                </div>
                                <div className="flex gap-2 items-center">
                                    <label className="flex flex-col flex-1">
                                        <span className={labelClass}>Twój numer telefonu</span>
                                        <input
                                            type="tel"
                                            placeholder="np. +49 151 12345678"
                                            value={regPhone}
                                            onChange={e => setRegPhone(e.target.value)}
                                            className={inputClass}
                                            required
                                        />
                                        <span className="text-[10px] text-slate-400 mt-1">
                                            Prefiks +49 zostanie dodany automatycznie.
                                        </span>
                                    </label>
                                    <button
                                        type="submit"
                                        disabled={loading}
                                        className="h-9 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-[12px] border-none rounded-[9px] cursor-pointer transition-colors disabled:opacity-50 shrink-0"
                                    >
                                        {loading ? 'Rejestrowanie...' : 'Zarejestruj'}
                                    </button>
                                </div>
                            </form>

                            {/* Alias list */}
                            <div className="flex flex-col gap-2">
                                <p className="text-[13px] font-semibold text-slate-800">Aktywne aliasy</p>
                                {registeredAliases.length === 0 ? (
                                    <div className="text-center py-8 border border-dashed border-slate-200 rounded-lg text-slate-400 text-[12px]">
                                        Nie zarejestrowano jeszcze żadnego numeru telefonu w systemie KLIK.
                                    </div>
                                ) : (
                                    <div className="flex flex-col gap-2">
                                        {registeredAliases.map(alias => (
                                            <div
                                                key={alias.phone}
                                                className="flex items-center justify-between bg-white border border-slate-200 rounded-lg px-4 py-3"
                                            >
                                                <div className="flex items-center gap-3">
                                                    <div className="w-8 h-8 rounded-lg bg-emerald-50 flex items-center justify-center shrink-0">
                                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                                                            <path d="M5 13l4 4L19 7" stroke="#059669" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                                        </svg>
                                                    </div>
                                                    <div>
                                                        <p className="text-[12px] font-semibold text-slate-800 font-mono">{alias.phone}</p>
                                                        <p className="text-[10px] text-slate-400">Rachunek: {alias.iban}</p>
                                                    </div>
                                                </div>
                                                <button
                                                    onClick={() => handleUnregisterAlias(alias.phone)}
                                                    disabled={loading}
                                                    className="text-[11px] font-medium text-rose-600 hover:text-rose-800 bg-transparent border-none cursor-pointer disabled:opacity-50 transition-colors"
                                                >
                                                    Wyrejestruj
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Pending transaction modal */}
            {pendingTx && (
                <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
                    <div className="bg-white rounded-lg border border-slate-200 shadow-2xl max-w-sm w-full p-6 flex flex-col gap-5">
                        <div className="flex items-center gap-3">
                            <div className="w-11 h-11 bg-amber-50 rounded-lg flex items-center justify-center shrink-0">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" stroke="#d97706" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
                                </svg>
                            </div>
                            <div>
                                <p className="text-[14px] font-semibold text-slate-900">Prośba o płatność KLIK</p>
                                <p className="text-[11px] text-slate-400">Wymaga autoryzacji</p>
                            </div>
                        </div>

                        <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 flex flex-col gap-2.5">
                            <div className="flex justify-between items-center">
                                <span className="text-[12px] text-slate-500">Kwota:</span>
                                <span className="text-[14px] font-bold text-slate-900">
                                    {formatEur(pendingTx.amount, pendingTx.currency)}
                                </span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-[12px] text-slate-500">Odbiorca:</span>
                                <span className="text-[12px] font-semibold text-slate-800 truncate max-w-[180px]">
                                    {pendingTx.merchantName}
                                </span>
                            </div>
                        </div>

                        <p className="text-[11px] text-slate-400 text-center">
                            Potwierdzając, wyrażasz zgodę na obciążenie rachunku wskazaną kwotą.
                        </p>

                        <div className="flex gap-2">
                            <button
                                onClick={() => handleConfirmTransaction('REJECTED')}
                                disabled={loading}
                                className="flex-1 h-10 bg-slate-100 hover:bg-slate-200 border-none rounded-lg text-slate-700 font-semibold text-[13px] cursor-pointer transition-colors disabled:opacity-50"
                            >
                                Odrzuć
                            </button>
                            <button
                                onClick={() => handleConfirmTransaction('ACCEPTED')}
                                disabled={loading}
                                className="flex-1 h-10 bg-blue-600 hover:bg-blue-700 border-none rounded-lg text-white font-semibold text-[13px] cursor-pointer transition-colors disabled:opacity-50"
                            >
                                {loading ? '...' : 'Potwierdź'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
