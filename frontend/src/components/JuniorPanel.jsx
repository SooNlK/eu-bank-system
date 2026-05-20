import { useState, useEffect } from 'react'
import { getJuniorAccounts, createJuniorAccount, getPendingApprovals, approveTransfer, rejectTransfer, getMyAccounts } from '../services/account'

function formatEur(amount) {
    return `€ ${amount.toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatIBAN(iban) {
    if (!iban) return ''
    return iban.replace(/\s+/g, '').replace(/(.{4})/g, '$1 ').trim()
}

export default function JuniorPanel() {
    const [juniorAccounts, setJuniorAccounts] = useState([])
    const [pendingTransfers, setPendingTransfers] = useState([])
    const [parentAccounts, setParentAccounts] = useState([])
    
    const [loading, setLoading] = useState(true)
    const [modalOpen, setModalOpen] = useState(false)
    const [actionLoading, setActionLoading] = useState(null) // store transfer ID being approved/rejected

    // Form State
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [dateOfBirth, setDateOfBirth] = useState('')
    const [passportNumber, setPassportNumber] = useState('')
    const [selectedParentAccountId, setSelectedParentAccountId] = useState('')
    const [formError, setFormError] = useState('')
    const [formSubmitting, setFormSubmitting] = useState(false)

    const loadData = async () => {
        try {
            setLoading(true)
            const [juniors, pending, parents] = await Promise.all([
                getJuniorAccounts(),
                getPendingApprovals(),
                getMyAccounts()
            ])
            setJuniorAccounts(juniors)
            setPendingTransfers(pending)
            setParentAccounts(parents)
            if (parents.length > 0) {
                setSelectedParentAccountId(parents[0].id)
            }
        } catch (err) {
            console.error('Failed to load Junior Panel data:', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadData()
    }, [])

    const handleApprove = async (transferId) => {
        setActionLoading(transferId)
        try {
            await approveTransfer(transferId)
            await loadData() // Refresh list & balances
        } catch (err) {
            alert(err.message || 'Nie udało się zatwierdzić przelewu')
        } finally {
            setActionLoading(null)
        }
    }

    const handleReject = async (transferId) => {
        setActionLoading(transferId)
        try {
            await rejectTransfer(transferId)
            await loadData() // Refresh list & balances
        } catch (err) {
            alert(err.message || 'Nie udało się odrzucić przelewu')
        } finally {
            setActionLoading(null)
        }
    }

    const validateForm = () => {
        if (!firstName.trim() || !lastName.trim() || !email.trim() || !password || !dateOfBirth || !passportNumber.trim() || !selectedParentAccountId) {
            return 'Wszystkie pola są wymagane.'
        }
        if (password.length < 8) {
            return 'Hasło musi mieć co najmniej 8 znaków.'
        }
        
        // Age validation (7-13 years old)
        const birthDate = new Date(dateOfBirth)
        const today = new Date()
        let age = today.getFullYear() - birthDate.getFullYear()
        const monthDiff = today.getMonth() - birthDate.getMonth()
        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
            age--
        }
        if (age < 7 || age > 13) {
            return 'Wiek dziecka musi wynosić od 7 do 13 lat.'
        }

        // Passport validation regex
        const passportRegex = /^[CFGHJKLMNPRTVWXYZcfghjklmnprtvwxyz0-9]{9}$/
        if (!passportRegex.test(passportNumber)) {
            return 'Numer paszportu/legitymacji musi mieć dokładnie 9 znaków i zawierać tylko cyfry oraz dozwolone litery.'
        }

        return ''
    }

    const handleCreateJunior = async (e) => {
        e.preventDefault()
        setFormError('')
        
        const validationMsg = validateForm()
        if (validationMsg) {
            setFormError(validationMsg)
            return
        }

        setFormSubmitting(true)
        try {
            await createJuniorAccount({
                email: email.trim().toLowerCase(),
                password,
                firstName: firstName.trim(),
                lastName: lastName.trim(),
                passportNumber: passportNumber.trim().toUpperCase(),
                dateOfBirth,
                parentAccountId: selectedParentAccountId
            })
            // Reset form
            setFirstName('')
            setLastName('')
            setEmail('')
            setPassword('')
            setDateOfBirth('')
            setPassportNumber('')
            setFormError('')
            setModalOpen(false)
            // Reload list
            await loadData()
        } catch (err) {
            setFormError(err.message || 'Wystąpił błąd podczas tworzenia konta Junior.')
        } finally {
            setFormSubmitting(false)
        }
    }

    if (loading) {
        return (
            <div className="flex flex-col gap-4 animate-pulse">
                <div className="h-12 bg-white rounded-2xl border border-slate-200/70 w-2/3 mb-2" />
                <div className="grid grid-cols-[2fr_1.5fr] gap-4">
                    <div className="h-64 bg-white rounded-2xl border border-slate-200/70" />
                    <div className="h-64 bg-white rounded-2xl border border-slate-200/70" />
                </div>
            </div>
        )
    }

    return (
        <div className="flex flex-col gap-5">
            {/* Nagłówek */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-[17px] font-semibold text-slate-800 flex items-center gap-2">
                        Strefa Rodzica
                    </h2>
                    <p className="text-[12px] text-slate-400">Zarządzaj kontami swoich dzieci i zatwierdzaj ich transakcje</p>
                </div>
                <button
                    onClick={() => {
                        setFormError('')
                        setModalOpen(true)
                    }}
                    className="bg-blue-600 hover:bg-blue-700 text-white text-[12.5px] font-medium border-none px-4 py-2 rounded-[10px] cursor-pointer shadow-sm transition-all flex items-center gap-1.5"
                >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    Założ konto Junior
                </button>
            </div>

            {/* Główny układ */}
            <div className="grid grid-cols-1 lg:grid-cols-[1.2fr_1fr] gap-5 items-start">
                
                {/* Kolumna 1: Konta dzieci */}
                <div className="bg-white rounded-2xl border border-slate-200/70 p-5 flex flex-col gap-4">
                    <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                        <h3 className="text-[13px] font-semibold text-slate-800 uppercase tracking-wider">
                            Konta Twoich dzieci ({juniorAccounts.length})
                        </h3>
                    </div>

                    {juniorAccounts.length === 0 ? (
                        <div className="text-center py-10 px-4 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                            <span className="text-[28px] block mb-2">🧸</span>
                            <p className="text-[13px] font-medium text-slate-700 mb-1">Brak kont typu Junior</p>
                            <p className="text-[11px] text-slate-400 max-w-[280px] mx-auto mb-4">
                                Nie posiadasz jeszcze powiązanych kont dla swoich dzieci. Załóż pierwsze konto, aby umożliwić im naukę oszczędzania!
                            </p>
                            <button
                                onClick={() => {
                                    setFormError('')
                                    setModalOpen(true)
                                }}
                                className="bg-slate-100 hover:bg-slate-200 text-slate-700 text-[11.5px] font-medium border border-slate-200 px-3.5 py-1.5 rounded-lg cursor-pointer transition-colors"
                            >
                                Załóż pierwsze konto
                            </button>
                        </div>
                    ) : (
                        <div className="grid gap-3.5">
                            {juniorAccounts.map((acc) => (
                                <div
                                    key={acc.id}
                                    className="border border-slate-100 hover:border-slate-200 rounded-xl p-4 bg-gradient-to-br from-white to-slate-50/20 flex flex-col sm:flex-row justify-between sm:items-center gap-3 transition-all"
                                >
                                    <div className="min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-[13px] font-bold text-slate-800">{acc.ownerName || 'Konto Dziecka'}</span>
                                            <span className="bg-purple-100 text-purple-700 text-[9px] font-semibold px-2 py-0.5 rounded-full uppercase">
                                                Junior 🧸
                                            </span>
                                        </div>
                                        <p className="text-[10px] text-slate-400 font-mono tracking-wider mb-0.5 truncate">
                                            {formatIBAN(acc.accountNumber)}
                                        </p>
                                        {acc.parentAccountId && (
                                            <p className="text-[9px] text-slate-400">
                                                Powiązane z Twoim kontem: <span className="font-mono">{formatIBAN(parentAccounts.find(p => p.id === acc.parentAccountId)?.accountNumber ?? '')}</span>
                                            </p>
                                        )}
                                    </div>
                                    <div className="text-left sm:text-right shrink-0">
                                        <p className="text-[16px] font-bold text-slate-900">{formatEur(acc.balance || 0)}</p>
                                        <p className="text-[9px] text-green-500 font-medium">Aktywne · Oszczędności</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Kolumna 2: Przelewy do zatwierdzenia */}
                <div className="bg-white rounded-2xl border border-slate-200/70 p-5 flex flex-col gap-4">
                    <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                        <h3 className="text-[13px] font-semibold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                            Oczekujące transakcje ({pendingTransfers.length})
                            {pendingTransfers.length > 0 && (
                                <span className="w-2.5 h-2.5 bg-amber-500 rounded-full animate-pulse" />
                            )}
                        </h3>
                    </div>

                    {pendingTransfers.length === 0 ? (
                        <div className="text-center py-10 px-4 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                            <span className="text-[28px] block mb-2">🌟</span>
                            <p className="text-[13px] font-medium text-slate-700 mb-0.5">Wszystko gotowe!</p>
                            <p className="text-[11px] text-slate-400 max-w-[240px] mx-auto">
                                Twoje dzieci nie mają obecnie żadnych transakcji oczekujących na zatwierdzenie.
                            </p>
                        </div>
                    ) : (
                        <div className="grid gap-3.5">
                            {pendingTransfers.map((tx) => {
                                const isBusy = actionLoading === tx.id
                                return (
                                    <div
                                        key={tx.id}
                                        className="border border-slate-100 rounded-xl p-4 bg-amber-50/15 border-l-4 border-l-amber-500 flex flex-col gap-3 transition-all"
                                    >
                                        <div className="flex items-start justify-between gap-2">
                                            <div className="min-w-0">
                                                <div className="flex items-center gap-1.5 mb-1.5">
                                                    <div className="w-5 h-5 rounded-full bg-amber-100 flex items-center justify-center text-[10px] text-amber-800 font-bold shrink-0">
                                                        👶
                                                    </div>
                                                    <span className="text-[11.5px] font-bold text-slate-700">
                                                        Zlecone przez: {tx.fromAccountOwner || 'Twoje Dziecko'}
                                                    </span>
                                                </div>
                                                <p className="text-[12px] font-semibold text-slate-800 truncate mb-1">
                                                    {tx.description || 'Przelew bankowy'}
                                                </p>
                                                <p className="text-[10px] text-slate-400 font-mono truncate">
                                                    Do: {tx.toIban}
                                                </p>
                                                <p className="text-[9.5px] text-slate-400 mt-1">
                                                    Data: {new Date(tx.createdAt || tx.valueDate).toLocaleDateString('pl-PL', { hour: '2-digit', minute: '2-digit' })}
                                                </p>
                                            </div>
                                            <div className="text-right shrink-0">
                                                <p className="text-[15px] font-extrabold text-slate-900">
                                                    {formatEur(tx.amount || 0)}
                                                </p>
                                                <span className="inline-block bg-amber-100 text-amber-700 text-[8px] font-bold px-1.5 py-0.5 rounded-full uppercase mt-1">
                                                    Oczekuje na zgodę
                                                </span>
                                            </div>
                                        </div>

                                        {/* Przyciski Akcji */}
                                        <div className="flex gap-2 border-t border-slate-100 pt-3 mt-1">
                                            <button
                                                disabled={isBusy}
                                                onClick={() => handleReject(tx.id)}
                                                className="flex-1 bg-red-50 hover:bg-red-100 text-red-600 text-[11.5px] font-medium border-none py-2 rounded-lg cursor-pointer transition-colors flex items-center justify-center gap-1"
                                            >
                                                {isBusy ? '...' : (
                                                    <>
                                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                                                            <line x1="18" y1="6" x2="6" y2="18" />
                                                            <line x1="6" y1="6" x2="18" y2="18" />
                                                        </svg>
                                                        Odrzuć
                                                    </>
                                                )}
                                            </button>
                                            <button
                                                disabled={isBusy}
                                                onClick={() => handleApprove(tx.id)}
                                                className="flex-1 bg-green-50 hover:bg-green-100 text-green-600 text-[11.5px] font-medium border-none py-2 rounded-lg cursor-pointer transition-colors flex items-center justify-center gap-1"
                                            >
                                                {isBusy ? '...' : (
                                                    <>
                                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                                                            <polyline points="20 6 9 17 4 12" />
                                                        </svg>
                                                        Zatwierdź
                                                    </>
                                                )}
                                            </button>
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    )}
                </div>
            </div>

            {/* Modal Rejestracji Juniora */}
            {modalOpen && (
                <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
                    <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-md w-full overflow-hidden animate-in fade-in zoom-in duration-200">
                        {/* Header */}
                        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                            <div>
                                <h3 className="text-[14px] font-bold text-slate-800 flex items-center gap-1.5">
                                    Załóż konto Junior 🧸
                                </h3>
                                <p className="text-[10px] text-slate-400">Utwórz bezpieczne konto bankowe dla swojego dziecka</p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setModalOpen(false)}
                                className="bg-transparent border-none cursor-pointer text-slate-400 hover:text-slate-600 text-[18px]"
                            >
                                ×
                            </button>
                        </div>

                        {/* Form */}
                        <form onSubmit={handleCreateJunior} className="p-5 flex flex-col gap-3.5">
                            
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="text-[10.5px] text-slate-500 font-medium mb-1 block">Imię dziecka</label>
                                    <input
                                        type="text"
                                        required
                                        value={firstName}
                                        onChange={(e) => setFirstName(e.target.value)}
                                        className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-1.5 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors"
                                        placeholder="np. Tomek"
                                    />
                                </div>
                                <div>
                                    <label className="text-[10.5px] text-slate-500 font-medium mb-1 block">Nazwisko dziecka</label>
                                    <input
                                        type="text"
                                        required
                                        value={lastName}
                                        onChange={(e) => setLastName(e.target.value)}
                                        className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-1.5 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors"
                                        placeholder="np. Kowalski"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="text-[10.5px] text-slate-500 font-medium mb-1 block">Adres e-mail dziecka (login)</label>
                                <input
                                    type="email"
                                    required
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-1.5 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors"
                                    placeholder="np. tomek@example.com"
                                />
                            </div>

                            <div>
                                <label className="text-[10.5px] text-slate-500 font-medium mb-1 block">Hasło do logowania dziecka</label>
                                <input
                                    type="password"
                                    required
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-1.5 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors"
                                    placeholder="Min. 8 znaków"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="text-[10.5px] text-slate-500 font-medium mb-1 block">Data urodzenia (wiek 7-13)</label>
                                    <input
                                        type="date"
                                        required
                                        value={dateOfBirth}
                                        onChange={(e) => setDateOfBirth(e.target.value)}
                                        className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-1.5 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors"
                                    />
                                </div>
                                <div>
                                    <label className="text-[10.5px] text-slate-500 font-medium mb-1 block">Paszport lub legitymacja</label>
                                    <input
                                        type="text"
                                        required
                                        maxLength={9}
                                        value={passportNumber}
                                        onChange={(e) => setPassportNumber(e.target.value)}
                                        className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-1.5 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors font-mono uppercase"
                                        placeholder="np. C01X00T47"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="text-[10.5px] text-slate-500 font-medium mb-1 block">Powiąż z Twoim rachunkiem</label>
                                <select
                                    required
                                    value={selectedParentAccountId}
                                    onChange={(e) => setSelectedParentAccountId(e.target.value)}
                                    className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-1.5 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors"
                                >
                                    {parentAccounts.map((p) => (
                                        <option key={p.id} value={p.id}>
                                            {p.type === 'STANDARD' ? 'Rachunek główny' : p.type} ({formatEur(p.balance)}) - {p.accountNumber}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {formError && (
                                <p className="text-[11px] text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2 mt-1">
                                    ⚠️ {formError}
                                </p>
                            )}

                            {/* Actions */}
                            <div className="flex gap-2 border-t border-slate-100 pt-4 mt-2">
                                <button
                                    type="button"
                                    onClick={() => setModalOpen(false)}
                                    className="flex-1 border border-slate-200 rounded-[9px] px-4 py-2 text-[12px] font-medium text-slate-700 bg-white cursor-pointer hover:bg-slate-50 transition-colors"
                                >
                                    Anuluj
                                </button>
                                <button
                                    type="submit"
                                    disabled={formSubmitting}
                                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white border-none rounded-[9px] px-4 py-2 text-[12px] font-medium cursor-pointer transition-colors shadow-sm"
                                >
                                    {formSubmitting ? 'Zakładanie...' : 'Utwórz konto'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    )
}
