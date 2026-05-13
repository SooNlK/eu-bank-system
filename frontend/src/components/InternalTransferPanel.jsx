import { useState } from 'react'

const ACCOUNTS = [
    {
        id: 'main',
        label: 'Rachunek główny',
        iban: 'PL61 1090 1014 0000 0712 1981 2874',
        balance: 18420.0,
        currency: 'EUR',
        color: '#1a3c8f',
    },
    {
        id: 'savings',
        label: 'Konto oszczędnościowe',
        iban: 'PL83 1090 2590 0000 0001 4200 4000',
        balance: 6430.37,
        currency: 'EUR',
        color: '#0f766e',
    },
]

function formatCurrency(value, currency = 'EUR') {
    return new Intl.NumberFormat('pl-PL', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
    }).format(value)
}

function AccountOption({ account, selected, onSelect, disabled }) {
    return (
        <button
            type="button"
            onClick={() => !disabled && onSelect(account.id)}
            disabled={disabled}
            className={`w-full text-left rounded-[11px] border p-3 flex items-center gap-3 transition-all duration-150
                ${disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}
                ${selected
                    ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500/20'
                    : disabled
                        ? 'border-slate-200 bg-slate-50'
                        : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                }`}
        >
            <div
                className="w-9 h-9 rounded-[9px] flex items-center justify-center shrink-0"
                style={{ background: account.color + '18' }}
            >
                <WalletIcon color={account.color} />
            </div>
            <div className="min-w-0 flex-1">
                <p className="text-[12px] font-medium text-slate-800 truncate">{account.label}</p>
                <p className="text-[10px] text-slate-400 truncate font-mono">{account.iban}</p>
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
    'w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400 transition-colors'
const labelClass = 'text-[11px] text-slate-500 font-medium mb-1.5 block'

export default function InternalTransferPanel({ onClose }) {
    const [fromId, setFromId] = useState(ACCOUNTS[0].id)
    const [toId, setToId] = useState(ACCOUNTS[1].id)
    const [amount, setAmount] = useState('')
    const [note, setNote] = useState('')
    const [status, setStatus] = useState(null) // null | 'success' | 'error'
    const [loading, setLoading] = useState(false)

    const fromAccount = ACCOUNTS.find((a) => a.id === fromId)
    const toAccount = ACCOUNTS.find((a) => a.id === toId)

    const handleSwap = () => {
        setFromId(toId)
        setToId(fromId)
    }

    const amountNum = parseFloat(amount)
    const isAmountValid = !isNaN(amountNum) && amountNum > 0
    const isOverBalance = isAmountValid && amountNum > fromAccount.balance
    const canSubmit = isAmountValid && !isOverBalance && fromId !== toId

    const handleSubmit = (e) => {
        e.preventDefault()
        if (!canSubmit) return
        setLoading(true)
        // Symulacja wysłania
        setTimeout(() => {
            setLoading(false)
            setStatus('success')
        }, 1200)
    }

    if (status === 'success') {
        return (
            <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4 flex flex-col items-center justify-center gap-3 py-10">
                <div className="w-14 h-14 bg-green-50 rounded-full flex items-center justify-center mb-1">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="12" r="10" stroke="#16a34a" strokeWidth="1.8" />
                        <path d="M7.5 12l3 3 6-6" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                </div>
                <p className="text-[15px] font-semibold text-slate-800">Przelew zlecony</p>
                <p className="text-[12px] text-slate-500 text-center max-w-[260px]">
                    Środki w kwocie <strong>{formatCurrency(amountNum)}</strong> zostały
                    przelane z <strong>{fromAccount.label}</strong> na{' '}
                    <strong>{toAccount.label}</strong>.
                </p>
                <button
                    onClick={onClose}
                    className="mt-2 bg-blue-600 text-white border-none rounded-[9px] px-6 py-2 text-[13px] font-medium cursor-pointer hover:bg-blue-700 transition-colors"
                >
                    Zamknij
                </button>
            </div>
        )
    }

    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4">
            {/* Header */}
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
                    ✕
                </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
                {/* Z rachunku */}
                <div>
                    <label className={labelClass}>Z rachunku</label>
                    <div className="flex flex-col gap-2">
                        {ACCOUNTS.map((acc) => (
                            <AccountOption
                                key={acc.id}
                                account={acc}
                                selected={fromId === acc.id}
                                onSelect={setFromId}
                                disabled={acc.id === toId}
                            />
                        ))}
                    </div>
                </div>

                {/* Swap button */}
                <div className="flex items-center gap-3">
                    <div className="flex-1 h-px bg-slate-100" />
                    <button
                        type="button"
                        onClick={handleSwap}
                        title="Zamień konta"
                        className="w-8 h-8 rounded-full border border-slate-200 bg-white flex items-center justify-center cursor-pointer hover:bg-slate-50 hover:border-slate-300 transition-all shrink-0"
                    >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                            <path d="M7 16V4m0 0L3 8m4-4l4 4" stroke="#64748b" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                            <path d="M17 8v12m0 0l4-4m-4 4l-4-4" stroke="#64748b" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                    </button>
                    <div className="flex-1 h-px bg-slate-100" />
                </div>

                {/* Na rachunek */}
                <div>
                    <label className={labelClass}>Na rachunek</label>
                    <div className="flex flex-col gap-2">
                        {ACCOUNTS.map((acc) => (
                            <AccountOption
                                key={acc.id}
                                account={acc}
                                selected={toId === acc.id}
                                onSelect={setToId}
                                disabled={acc.id === fromId}
                            />
                        ))}
                    </div>
                </div>

                {/* Kwota */}
                <div>
                    <label className={labelClass}>Kwota (EUR)</label>
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
                            Kwota przekracza dostępne saldo ({formatCurrency(fromAccount.balance)})
                        </p>
                    )}
                    {isAmountValid && !isOverBalance && (
                        <p className="text-[11px] text-slate-400 mt-1">
                            Dostępne saldo: {formatCurrency(fromAccount.balance)}
                        </p>
                    )}
                </div>

                {/* Tytuł (opcjonalny) */}
                <div>
                    <label className={labelClass}>
                        Tytuł <span className="text-slate-400 font-normal">(opcjonalnie)</span>
                    </label>
                    <input
                        type="text"
                        value={note}
                        onChange={(e) => setNote(e.target.value)}
                        placeholder="np. zasilenie oszczędności"
                        maxLength={140}
                        className={inputClass}
                    />
                </div>

                {/* Buttons */}
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
                        disabled={!canSubmit || loading}
                        className={`rounded-[9px] px-5 py-2 text-[13px] font-medium border-none transition-colors
                            ${canSubmit && !loading
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
                                Przetwarzanie…
                            </span>
                        ) : (
                            'Przesuń środki'
                        )}
                    </button>
                </div>
            </form>
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
