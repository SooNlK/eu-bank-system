import { useEffect, useState } from 'react'
import { createInternalTransfer, getMyAccounts } from '../services/account'

function formatCurrency(value, currency = 'EUR') {
    return new Intl.NumberFormat('pl-PL', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
    }).format(value)
}

function accountLabel(account) {
    return account.type === 'STANDARD' ? 'Rachunek główny' : account.type
}

function AccountOption({ account, selected, onSelect, disabled }) {
    const color = selected ? '#1a3c8f' : '#64748b'

    return (
        <button
            type="button"
            onClick={() => !disabled && onSelect(account.id)}
            disabled={disabled}
            className={`w-full text-left rounded-[11px] border p-3 flex items-center gap-3 transition-all duration-150
                ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                ${selected
                    ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500/20'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                }`}
        >
            <div
                className="w-9 h-9 rounded-[9px] flex items-center justify-center shrink-0"
                style={{ background: `${color}18` }}
            >
                <WalletIcon color={color} />
            </div>
            <div className="min-w-0 flex-1">
                <p className="text-[12px] font-medium text-slate-800 truncate">{accountLabel(account)}</p>
                <p className="text-[10px] text-slate-400 truncate font-mono">{account.accountNumber}</p>
            </div>
            <div className="text-right shrink-0">
                <p className="text-[13px] font-semibold text-slate-800">
                    {formatCurrency(account.balance, account.currency)}
                </p>
                <p className="text-[10px] text-slate-400">{account.currency}</p>
            </div>
        </button>
    )
}

const inputClass =
    'w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors disabled:text-slate-400 disabled:cursor-not-allowed'
const labelClass = 'text-[11px] text-slate-500 font-medium mb-1.5 block'

export default function InternalTransferPanel({ onClose, onDashboardReturn }) {
    const [accounts, setAccounts] = useState([])
    const [fromId, setFromId] = useState('')
    const [toIban, setToIban] = useState('')
    const [amount, setAmount] = useState('')
    const [note, setNote] = useState('')
    const [transfer, setTransfer] = useState(null)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)
    const [initializing, setInitializing] = useState(true)

    useEffect(() => {
        async function loadAccounts() {
            try {
                const nextAccounts = await getMyAccounts()
                setAccounts(nextAccounts)
                setFromId(nextAccounts[0]?.id || '')
            } catch (err) {
                setError(err.message || 'Nie udało się pobrać rachunków')
            } finally {
                setInitializing(false)
            }
        }

        loadAccounts()
    }, [])

    const fromAccount = accounts.find((a) => a.id === fromId)
    const today = new Date().toISOString().slice(0, 10)
    const amountNum = parseFloat(amount)
    const isAmountValid = !isNaN(amountNum) && amountNum > 0
    const isOverBalance = Boolean(fromAccount) && isAmountValid && amountNum > fromAccount.balance
    const normalizedIban = toIban.replace(/\s+/g, '').toUpperCase()
    const canSubmit = Boolean(fromAccount) && isAmountValid && !isOverBalance && normalizedIban.length >= 15 && !loading

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (!canSubmit) return

        setLoading(true)
        setError('')

        try {
            const result = await createInternalTransfer({
                fromAccountId: fromId,
                toIban: normalizedIban,
                amount: amountNum,
                currency: fromAccount.currency,
                valueDate: today,
                description: note,
            })
            setTransfer(result)
            setAccounts(await getMyAccounts())
        } catch (err) {
            setError(err.message || 'Nie udało się zlecić przelewu')
        } finally {
            setLoading(false)
        }
    }

    if (transfer) {
        const completed = transfer.status === 'COMPLETED'

        return (
            <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4 flex flex-col items-center justify-center gap-3 py-10">
                <div className={`w-14 h-14 ${completed ? 'bg-green-50' : 'bg-red-50'} rounded-full flex items-center justify-center mb-1`}>
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="12" r="10" stroke={completed ? '#16a34a' : '#dc2626'} strokeWidth="1.8" />
                        {completed ? (
                            <path d="M7.5 12l3 3 6-6" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        ) : (
                            <path d="M8 8l8 8M16 8l-8 8" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" />
                        )}
                    </svg>
                </div>
                <p className="text-[15px] font-semibold text-slate-800">
                    {completed ? 'Przelew wykonany' : 'Przelew nie został wykonany'}
                </p>
                <p className="text-[12px] text-slate-500 text-center max-w-[280px]">
                    Status: <strong>{transfer.status}</strong>. Kwota:{' '}
                    <strong>{formatCurrency(transfer.amount, transfer.currency)}</strong>.
                </p>
                <div className="flex gap-3 mt-4 w-full px-4 sm:px-0 sm:w-auto">
                    <button
                        onClick={() => {
                            setToIban('');
                            setAmount('');
                            setNote('');
                            setTransfer(null);
                        }}
                        className="flex-1 sm:flex-none bg-slate-100 text-slate-700 border-none rounded-[9px] px-5 py-2.5 sm:py-2 text-[13px] font-medium cursor-pointer hover:bg-slate-200 transition-colors"
                    >
                        Nowy przelew
                    </button>
                    <button
                        onClick={onDashboardReturn || onClose}
                        className="flex-1 sm:flex-none bg-blue-600 text-white border-none rounded-[9px] px-5 py-2.5 sm:py-2 text-[13px] font-medium cursor-pointer hover:bg-blue-700 transition-colors shadow-sm"
                    >
                        Wróć do pulpitu
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4">
            <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 bg-indigo-50 rounded-lg flex items-center justify-center">
                        <ArrowsIcon />
                    </div>
                    <p className="text-[14px] font-medium text-slate-800">Przelew wewnętrzny</p>
                </div>
                <button
                    type="button"
                    onClick={onClose}
                    className="bg-transparent border-none cursor-pointer text-slate-400 text-[18px] leading-none hover:text-slate-600"
                    aria-label="Zamknij"
                >
                    x
                </button>
            </div>

            {initializing ? (
                <div className="h-56 rounded-xl bg-slate-100 animate-pulse" />
            ) : (
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className={labelClass}>Z rachunku</label>
                        <div className="flex flex-col gap-2">
                            {accounts.map((acc) => (
                                <AccountOption
                                    key={acc.id}
                                    account={acc}
                                    selected={fromId === acc.id}
                                    onSelect={setFromId}
                                    disabled={loading}
                                />
                            ))}
                        </div>
                    </div>

                    <div>
                        <label className={labelClass}>IBAN odbiorcy w tym banku</label>
                        <input
                            type="text"
                            value={toIban}
                            onChange={(e) => setToIban(e.target.value)}
                            className={`${inputClass} font-mono uppercase`}
                            autoComplete="off"
                            required
                        />
                    </div>

                    <div>
                        <label className={labelClass}>Kwota ({fromAccount?.currency || 'EUR'})</label>
                        <div className="relative">
                            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[13px] text-slate-400 font-medium pointer-events-none">
                                €
                            </span>
                            <input
                                type="number"
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                                placeholder="0,00"
                                min="0.01"
                                step="0.01"
                                className={`${inputClass} pl-7`}
                                required
                            />
                        </div>
                        {isOverBalance && (
                            <p className="text-[11px] text-red-500 mt-1 flex items-center gap-1">
                                <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
                                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                                    <path d="M12 8v4m0 4h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                </svg>
                                Kwota przekracza dostępne saldo ({formatCurrency(fromAccount.balance, fromAccount.currency)})
                            </p>
                        )}
                        {isAmountValid && !isOverBalance && (
                            <p className="text-[11px] text-slate-400 mt-1">
                                Dostępne saldo: {formatCurrency(fromAccount?.balance || 0, fromAccount?.currency || 'EUR')}
                            </p>
                        )}
                    </div>

                    <div>
                        <label className={labelClass}>Data waluty</label>
                        <input type="date" value={today} className={inputClass} disabled />
                    </div>

                    <div>
                        <label className={labelClass}>
                            Tytuł <span className="text-slate-400 font-normal">(opcjonalnie)</span>
                        </label>
                        <input
                            type="text"
                            value={note}
                            onChange={(e) => setNote(e.target.value)}
                            maxLength={140}
                            className={inputClass}
                        />
                    </div>

                    {error && (
                        <p className="text-[12px] text-red-600 bg-red-50 border border-red-100 rounded-[9px] px-3 py-2">
                            {error}
                        </p>
                    )}

                    <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 pt-1">
                        <button
                            type="button"
                            onClick={onClose}
                            className="border border-slate-200 rounded-[9px] px-5 py-2 text-[13px] font-medium text-slate-700 bg-white cursor-pointer hover:bg-slate-50 transition-colors"
                        >
                            Anuluj
                        </button>
                        <button
                            type="submit"
                            disabled={!canSubmit}
                            className={`rounded-[9px] px-5 py-2 text-[13px] font-medium border-none transition-colors
                                ${canSubmit
                                    ? 'bg-blue-600 text-white cursor-pointer hover:bg-blue-700'
                                    : 'bg-slate-100 text-slate-400 cursor-not-allowed'
                                }`}
                        >
                            {loading ? (
                                <span className="flex items-center gap-2">
                                    <svg className="animate-spin" width="13" height="13" viewBox="0 0 24 24" fill="none">
                                        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" opacity="0.25" />
                                        <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                    Przetwarzanie...
                                </span>
                            ) : (
                                'Wyślij przelew'
                            )}
                        </button>
                    </div>
                </form>
            )}
        </div>
    )
}

function WalletIcon({ color = '#1a3c8f' }) {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <rect x="2" y="7" width="20" height="14" rx="2" stroke={color} strokeWidth="1.8" />
            <path d="M16 14a1 1 0 1 1 0-2 1 1 0 0 1 0 2z" fill={color} />
            <path d="M6 7V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2" stroke={color} strokeWidth="1.8" />
        </svg>
    )
}

function ArrowsIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M7 16V4m0 0L3 8m4-4l4 4" stroke="#6366f1" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            <path d="M17 8v12m0 0l4-4m-4 4l-4-4" stroke="#6366f1" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    )
}
