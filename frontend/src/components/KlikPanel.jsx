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

export default function KlikPanel() {
    const [accounts, setAccounts] = useState([])
    const [selectedAccount, setSelectedAccount] = useState('')
    const [activeTab, setActiveTab] = useState('c2b')
    const [loading, setLoading] = useState(false)
    const [toast, setToast] = useState(null)

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
            return JSON.parse(localStorage.getItem('klik_registered_aliases') || '[]')
        } catch {
            return []
        }
    })

    useEffect(() => {
        async function loadAccounts() {
            try {
                const accs = await getMyAccounts()
                setAccounts(accs)
                if (accs.length > 0) {
                    setSelectedAccount(accs[0].id)
                }
            } catch (err) {
                showToast(err.message, 'error')
            }
        }
        loadAccounts()

        return () => {
            clearInterval(timerRef.current)
            clearInterval(pollRef.current)
        }
    }, [])

    useEffect(() => {
        localStorage.setItem('klik_registered_aliases', JSON.stringify(registeredAliases))
    }, [registeredAliases])

    function showToast(message, type = 'success') {
        setToast({ message, type })
        setTimeout(() => setToast(null), 4000)
    }

    // --- C2B Logic ---

    async function handleGenerateCode() {
        if (!selectedAccount) {
            showToast('Proszę wybrać rachunek', 'error')
            return
        }
        setLoading(true)
        try {
            const res = await generateKlikCode(selectedAccount)
            setKlikCode(res.code)
            setTimeLeft(120)

            // Start countdown timer
            clearInterval(timerRef.current)
            timerRef.current = setInterval(() => {
                setTimeLeft((prev) => {
                    if (prev <= 1) {
                        clearInterval(timerRef.current)
                        clearInterval(pollRef.current)
                        setKlikCode('')
                        return 0
                    }
                    return prev - 1
                })
            }, 1000)

            // Start polling for pending authorization webhook
            startPollingPending()
        } catch (err) {
            showToast(err.message, 'error')
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
                    // Get the latest pending transaction
                    setPendingTx(pending[0])
                    clearInterval(pollRef.current) // Stop polling once we find a request
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
            showToast(
                status === 'ACCEPTED' 
                    ? 'Płatność zaakceptowana pomyślnie! Środki zostały zaksięgowane.' 
                    : 'Płatność została odrzucona.',
                status === 'ACCEPTED' ? 'success' : 'warning'
            )
            // Reset states
            setPendingTx(null)
            setKlikCode('')
            setTimeLeft(0)
            clearInterval(timerRef.current)
        } catch (err) {
            showToast(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    // --- P2P Logic ---

    async function handleP2pTransfer(e) {
        e.preventDefault()
        if (!selectedAccount || !p2pPhone || !p2pAmount) {
            showToast('Wypełnij wszystkie pola formularza', 'error')
            return
        }
        setLoading(true)
        try {
            const res = await createKlikP2pTransfer({
                fromAccountId: selectedAccount,
                toPhone: p2pPhone,
                amount: parseFloat(p2pAmount),
                currency: 'EUR',
                description: p2pDescription
            })
            showToast(`Przelew zrealizowany pomyślnie! Odbiorca otrzymał środki. ID: ${res.id}`)
            // Reset form
            setP2pPhone('')
            setP2pAmount('')
            setP2pDescription('Przelew na telefon KLIK')
        } catch (err) {
            showToast(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    // --- Alias Logic ---

    async function handleRegisterAlias(e) {
        e.preventDefault()
        if (!selectedAccount || !regPhone) {
            showToast('Podaj numer telefonu', 'error')
            return
        }
        setLoading(true)
        try {
            const res = await registerKlikAlias(selectedAccount, regPhone)
            const acc = accounts.find(a => a.id === selectedAccount)
            const newAlias = {
                phone: regPhone,
                accountId: selectedAccount,
                iban: acc ? acc.accountNumber : 'N/A',
                registeredAt: res.registered_at || new Date().toISOString()
            }
            // Remove previous copy if exists and add new
            setRegisteredAliases(prev => [newAlias, ...prev.filter(a => a.phone !== regPhone)])
            showToast(`Numer telefonu ${regPhone} został pomyślnie zarejestrowany!`)
            setRegPhone('')
        } catch (err) {
            showToast(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    async function handleUnregisterAlias(phone) {
        setLoading(true)
        try {
            await unregisterKlikAlias(phone)
            setRegisteredAliases(prev => prev.filter(a => a.phone !== phone))
            showToast(`Numer telefonu ${phone} został wyrejestrowany.`)
        } catch (err) {
            showToast(err.message, 'error')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="flex flex-col gap-6 max-w-4xl mx-auto">
            {/* Header */}
            <div className="flex items-center justify-between bg-white rounded-2xl border border-slate-200/70 p-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
                        <span className="text-pink-600 font-extrabold tracking-wider">KLIK</span> Mobile
                    </h1>
                    <p className="text-[13px] text-slate-500 mt-1">Generuj kody płatności lub wysyłaj przelewy błyskawiczne na numer telefonu.</p>
                </div>
                {/* Account selector */}
                <div className="flex flex-col gap-1">
                    <label className="text-[11px] font-medium text-slate-400 uppercase tracking-wider">Aktywny Rachunek</label>
                    <select
                        value={selectedAccount}
                        onChange={(e) => setSelectedAccount(e.target.value)}
                        className="bg-slate-50 border border-slate-200 text-slate-700 text-[13px] rounded-xl px-3 py-2 outline-none font-medium cursor-pointer"
                    >
                        {accounts.map(acc => (
                            <option key={acc.id} value={acc.id}>
                                {acc.type === 'JUNIOR' ? '🧸 ' : ''}{acc.accountNumber} ({acc.balance.toFixed(2)} {acc.currency})
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* Navigation tabs */}
            <div className="flex gap-2 bg-slate-200/50 p-1.5 rounded-2xl border border-slate-200/40">
                <button
                    onClick={() => setActiveTab('c2b')}
                    className={`flex-1 py-2.5 rounded-xl border-none font-medium text-[13px] cursor-pointer transition-all duration-150
                        ${activeTab === 'c2b' ? 'bg-white text-slate-800 shadow-sm' : 'bg-transparent text-slate-500 hover:text-slate-700'}`}
                >
                    📱 Kod KLIK (Płatność)
                </button>
                <button
                    onClick={() => setActiveTab('p2p')}
                    className={`flex-1 py-2.5 rounded-xl border-none font-medium text-[13px] cursor-pointer transition-all duration-150
                        ${activeTab === 'p2p' ? 'bg-white text-slate-800 shadow-sm' : 'bg-transparent text-slate-500 hover:text-slate-700'}`}
                >
                    💸 Przelew na telefon
                </button>
                <button
                    onClick={() => setActiveTab('aliases')}
                    className={`flex-1 py-2.5 rounded-xl border-none font-medium text-[13px] cursor-pointer transition-all duration-150
                        ${activeTab === 'aliases' ? 'bg-white text-slate-800 shadow-sm' : 'bg-transparent text-slate-500 hover:text-slate-700'}`}
                >
                    ⚙️ Zarządzaj aliasami
                </button>
            </div>

            {/* Tab Contents */}
            <div className="bg-white rounded-2xl border border-slate-200/70 p-6 min-h-[350px] flex flex-col">
                {activeTab === 'c2b' && (
                    <div className="flex flex-col items-center justify-center flex-1 py-4">
                        {klikCode ? (
                            <div className="flex flex-col items-center gap-6">
                                <p className="text-[12px] font-semibold text-slate-400 uppercase tracking-widest">Twój jednorazowy kod KLIK</p>
                                <div className="bg-slate-50 border border-slate-200 rounded-3xl px-8 py-5 flex items-center justify-center shadow-inner">
                                    <span className="text-5xl font-mono font-bold tracking-[0.2em] text-slate-800 pl-[0.2em]">
                                        {klikCode}
                                    </span>
                                </div>
                                <div className="flex flex-col items-center gap-1.5 w-full max-w-xs">
                                    <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                                        <div 
                                            className="bg-pink-600 h-full transition-all duration-1000 ease-linear"
                                            style={{ width: `${(timeLeft / 120) * 100}%` }}
                                        />
                                    </div>
                                    <p className="text-[13px] text-slate-500 font-medium">
                                        Kod wygaśnie za: <span className="text-slate-800 font-bold">{timeLeft} sekund</span>
                                    </p>
                                </div>
                                <p className="text-[11px] text-slate-400 text-center italic max-w-sm mt-2">
                                    Wpisz powyższy kod w terminalu płatniczym lub u agenta płatności i zatwierdź transakcję na telefonie.
                                </p>
                            </div>
                        ) : (
                            <div className="flex flex-col items-center gap-5 text-center">
                                <div className="w-16 h-16 bg-pink-50 rounded-2xl flex items-center justify-center text-3xl">
                                    📱
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-slate-800">Generowanie płatności kodem</h3>
                                    <p className="text-[13px] text-slate-500 max-w-sm mt-1">Generuje unikalny 6-cyfrowy kod, którym zapłacisz w sklepie internetowym lub stacjonarnym.</p>
                                </div>
                                <button
                                    onClick={handleGenerateCode}
                                    disabled={loading}
                                    className="bg-pink-600 hover:bg-pink-700 text-white font-semibold text-[13px] border-none rounded-xl px-6 py-3 cursor-pointer shadow-md shadow-pink-100 transition-all duration-150 disabled:opacity-50"
                                >
                                    {loading ? 'Generowanie...' : 'Generuj kod KLIK'}
                                </button>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'p2p' && (
                    <form onSubmit={handleP2pTransfer} className="flex flex-col gap-4 max-w-md mx-auto w-full">
                        <h3 className="text-[15px] font-bold text-slate-800">Wyślij przelew na telefon</h3>
                        
                        <div className="flex flex-col gap-1.5">
                            <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Numer telefonu odbiorcy</label>
                            <input
                                type="tel"
                                placeholder="np. +48123456789"
                                value={p2pPhone}
                                onChange={(e) => setP2pPhone(e.target.value)}
                                className="border border-slate-200 rounded-xl px-3.5 py-2.5 text-[13px] outline-none hover:border-slate-300 focus:border-blue-500 font-medium"
                                required
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="flex flex-col gap-1.5">
                                <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Kwota przelewu</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    min="0.01"
                                    placeholder="0.00"
                                    value={p2pAmount}
                                    onChange={(e) => setP2pAmount(e.target.value)}
                                    className="border border-slate-200 rounded-xl px-3.5 py-2.5 text-[13px] outline-none hover:border-slate-300 focus:border-blue-500 font-medium"
                                    required
                                />
                            </div>
                            <div className="flex flex-col gap-1.5">
                                <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Waluta</label>
                                <input
                                    type="text"
                                    value="EUR"
                                    className="border border-slate-200 bg-slate-50 text-slate-400 rounded-xl px-3.5 py-2.5 text-[13px] outline-none font-bold"
                                    disabled
                                />
                            </div>
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Tytuł przelewu</label>
                            <input
                                type="text"
                                value={p2pDescription}
                                onChange={(e) => setP2pDescription(e.target.value)}
                                className="border border-slate-200 rounded-xl px-3.5 py-2.5 text-[13px] outline-none hover:border-slate-300 focus:border-blue-500 font-medium"
                                required
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold text-[13px] border-none rounded-xl px-6 py-3 cursor-pointer mt-2 shadow-md shadow-blue-100 transition-all duration-150 disabled:opacity-50"
                        >
                            {loading ? 'Przetwarzanie...' : 'Wyślij przelew'}
                        </button>
                    </form>
                )}

                {activeTab === 'aliases' && (
                    <div className="flex flex-col gap-6">
                        {/* Registration Form */}
                        <form onSubmit={handleRegisterAlias} className="bg-slate-50 rounded-2xl border border-slate-200/50 p-5 flex flex-col gap-4">
                            <h3 className="text-[14px] font-bold text-slate-800">Zarejestruj nowy numer telefonu</h3>
                            <p className="text-[12px] text-slate-500 leading-snug">
                                Powiązanie numeru telefonu z Twoim kontem bankowym pozwoli innym użytkownikom na wysyłanie Ci natychmiastowych przelewów bez konieczności wpisywania numeru IBAN.
                            </p>
                            <div className="flex gap-3 items-end">
                                <div className="flex-1 flex flex-col gap-1.5">
                                    <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Twój numer telefonu</label>
                                    <input
                                        type="tel"
                                        placeholder="np. +48123456789"
                                        value={regPhone}
                                        onChange={(e) => setRegPhone(e.target.value)}
                                        className="border border-slate-200 bg-white rounded-xl px-3.5 py-2.5 text-[13px] outline-none hover:border-slate-300 focus:border-blue-500 font-medium"
                                        required
                                    />
                                </div>
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="bg-teal-600 hover:bg-teal-700 text-white font-semibold text-[13px] border-none rounded-xl px-6 py-3 cursor-pointer shadow-md shadow-teal-100 transition-all duration-150 h-[40px] disabled:opacity-50"
                                >
                                    {loading ? 'Rejestrowanie...' : 'Zarejestruj'}
                                </button>
                            </div>
                        </form>

                        {/* Alias list */}
                        <div className="flex flex-col gap-3">
                            <h3 className="text-[14px] font-bold text-slate-800">Aktywne numery zarejestrowane w KLIK</h3>
                            {registeredAliases.length === 0 ? (
                                <div className="text-center py-6 border border-dashed border-slate-200 rounded-2xl text-slate-400 text-[12px]">
                                    Nie zarejestrowano jeszcze żadnego numeru telefonu w systemie KLIK.
                                </div>
                            ) : (
                                <div className="flex flex-col gap-2">
                                    {registeredAliases.map((alias) => (
                                        <div key={alias.phone} className="flex items-center justify-between bg-white border border-slate-200 rounded-xl px-4 py-3.5 shadow-sm">
                                            <div className="flex items-center gap-3">
                                                <div className="w-8 h-8 rounded-full bg-teal-50 text-teal-600 flex items-center justify-center font-bold text-[14px]">
                                                    ✓
                                                </div>
                                                <div>
                                                    <p className="text-[13px] font-bold text-slate-800">{alias.phone}</p>
                                                    <p className="text-[11px] text-slate-400">Rachunek: {alias.iban}</p>
                                                </div>
                                            </div>
                                            <button
                                                onClick={() => handleUnregisterAlias(alias.phone)}
                                                disabled={loading}
                                                className="bg-transparent border-none text-red-500 hover:text-red-700 font-medium text-[12px] cursor-pointer"
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

            {/* Webhook Authorization Request Overlay Modal */}
            {pendingTx && (
                <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
                    <div className="bg-white rounded-3xl border border-slate-200 shadow-2xl max-w-sm w-full p-6 flex flex-col gap-5 animate-in zoom-in-95 duration-200">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-pink-100 text-pink-600 rounded-2xl flex items-center justify-center text-xl shrink-0 animate-bounce">
                                🔔
                            </div>
                            <div>
                                <h3 className="text-[16px] font-bold text-slate-800">Prośba o płatność KLIK</h3>
                                <p className="text-[12px] text-slate-400">Wymaga autoryzacji PIN-em w banku</p>
                            </div>
                        </div>

                        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 flex flex-col gap-2.5">
                            <div className="flex justify-between items-center text-[13px]">
                                <span className="text-slate-500">Kwota płatności:</span>
                                <span className="font-bold text-slate-800 text-[15px]">{pendingTx.amount.toFixed(2)} {pendingTx.currency}</span>
                            </div>
                            <div className="flex justify-between items-center text-[13px]">
                                <span className="text-slate-500">Odbiorca:</span>
                                <span className="font-semibold text-slate-800 truncate max-w-[180px]" title={pendingTx.merchantName}>
                                    {pendingTx.merchantName}
                                </span>
                            </div>
                        </div>

                        <p className="text-[11px] text-slate-400 text-center leading-normal">
                            Potwierdzając, wyrażasz zgodę na obciążenie Twojego rachunku wskazaną kwotą płatności.
                        </p>

                        <div className="flex gap-3 mt-1">
                            <button
                                onClick={() => handleConfirmTransaction('REJECTED')}
                                disabled={loading}
                                className="flex-1 py-3 bg-slate-100 hover:bg-slate-200 border-none rounded-xl text-slate-600 font-bold text-[13px] cursor-pointer transition-colors"
                            >
                                Odrzuć
                            </button>
                            <button
                                onClick={() => handleConfirmTransaction('ACCEPTED')}
                                disabled={loading}
                                className="flex-1 py-3 bg-pink-600 hover:bg-pink-700 border-none rounded-xl text-white font-bold text-[13px] cursor-pointer transition-colors shadow-md shadow-pink-100"
                            >
                                Potwierdź
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Floating toast notification inside panel */}
            {toast && (
                <div className={`fixed bottom-6 right-6 px-4 py-3 rounded-2xl border text-white shadow-lg text-[13px] z-[999] font-medium animate-in slide-in-from-bottom duration-200
                    ${toast.type === 'error' ? 'bg-red-600 border-red-500' : ''}
                    ${toast.type === 'warning' ? 'bg-amber-600 border-amber-500' : ''}
                    ${toast.type === 'success' ? 'bg-slate-900 border-slate-800' : ''}`}>
                    {toast.type === 'error' && '❌ '}
                    {toast.type === 'warning' && '⚠️ '}
                    {toast.type === 'success' && '✨ '}
                    {toast.message}
                </div>
            )}
        </div>
    )
}
