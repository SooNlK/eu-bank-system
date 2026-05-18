import { useEffect, useState } from 'react'
import { getTransactionDetails } from '../services/account'

function StatusBadge({ status }) {
    const map = {
        COMPLETED: { label: 'Zrealizowana', bg: 'bg-green-50', text: 'text-green-700', dot: 'bg-green-500' },
        PENDING:   { label: 'W trakcie',    bg: 'bg-amber-50',  text: 'text-amber-700',  dot: 'bg-amber-400' },
        FAILED:    { label: 'Odrzucona',    bg: 'bg-red-50',    text: 'text-red-700',    dot: 'bg-red-500'   },
    }
    const cfg = map[status] ?? { label: status, bg: 'bg-slate-50', text: 'text-slate-600', dot: 'bg-slate-400' }
    return (
        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold ${cfg.bg} ${cfg.text}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${cfg.dot}`} />
            {cfg.label}
        </span>
    )
}



function Row({ label, value, mono }) {
    return (
        <div className="flex items-start justify-between gap-4 py-3 border-b border-slate-100 last:border-b-0">
            <span className="text-[12px] text-slate-500 shrink-0">{label}</span>
            <span className={`text-[12px] font-medium text-slate-800 text-right break-all ${mono ? 'font-mono text-[11px]' : ''}`}>
                {value ?? '—'}
            </span>
        </div>
    )
}

export default function TransactionDetailModal({ accountId, transactionId, onClose }) {
    const [tx, setTx] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!accountId || !transactionId) return
        setLoading(true)
        setError(null)
        getTransactionDetails(accountId, transactionId)
            .then(setTx)
            .catch(e => setError(e.message))
            .finally(() => setLoading(false))
    }, [accountId, transactionId])

    // Zamknij po kliknięciu tła
    function handleBackdrop(e) {
        if (e.target === e.currentTarget) onClose()
    }

    const isExpense = tx?.type === 'DEBIT'
    const rawAmount = tx?.amount ?? 0
    const currency = tx?.currency ?? 'EUR'
    const amountStr = rawAmount != null
        ? new Intl.NumberFormat('pl-PL', { style: 'currency', currency }).format(rawAmount)
        : '—'

    const dateStr = tx?.createdAt
        ? new Date(tx.createdAt).toLocaleString('pl-PL', {
            day: 'numeric', month: 'long', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
          })
        : '—'

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[2px] animate-fade-in"
            onClick={handleBackdrop}
        >
            <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl shadow-slate-900/10 overflow-hidden animate-slide-up">

                {/* Header z gradientem */}
                <div className={`px-6 pt-7 pb-6 ${isExpense
                    ? 'bg-gradient-to-br from-red-500 to-rose-600'
                    : 'bg-gradient-to-br from-emerald-500 to-green-600'
                }`}>
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-white/70 text-[11px] font-medium mb-1 uppercase tracking-wider">
                                {isExpense ? 'Przelew wychodzący' : 'Przelew przychodzący'}
                            </p>
                            <p className="text-white text-3xl font-bold tracking-tight">
                                {isExpense ? '−' : '+'}{amountStr}
                            </p>
                            <p className="text-white/60 text-[11px] mt-1.5">{dateStr}</p>
                        </div>

                        {/* Ikona */}
                        <div className="w-11 h-11 rounded-full bg-white/15 flex items-center justify-center shrink-0">
                            {isExpense ? (
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <path d="M5 12h14M12 5l7 7-7 7" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                                </svg>
                            ) : (
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <path d="M19 12H5M12 19l-7-7 7-7" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                                </svg>
                            )}
                        </div>
                    </div>

                    {/* Pasek statusu */}
                    {!loading && tx && (
                        <div className="flex gap-2 mt-4">
                            <StatusBadge status={tx.status} />
                        </div>
                    )}
                </div>

                {/* Treść */}
                <div className="px-6 py-4">
                    {loading && (
                        <div className="animate-pulse flex flex-col gap-3 py-4">
                            <div className="h-4 bg-slate-100 rounded w-3/4" />
                            <div className="h-4 bg-slate-100 rounded w-1/2" />
                            <div className="h-4 bg-slate-100 rounded w-5/6" />
                            <div className="h-4 bg-slate-100 rounded w-2/3" />
                        </div>
                    )}
                    {error && (
                        <p className="text-[12px] text-red-500 py-4 text-center">Nie udało się pobrać szczegółów transakcji.</p>
                    )}
                    {!loading && tx && (
                        <div className="flex flex-col">
                            <Row label="Tytuł" value={tx.description} />
                            {tx.counterpartyName && (
                                <Row label={isExpense ? "Odbiorca" : "Nadawca"} value={tx.counterpartyName} />
                            )}
                            {tx.counterpartyIban && (
                                <Row label={isExpense ? "Rachunek odbiorcy" : "Rachunek nadawcy"} value={tx.counterpartyIban} mono />
                            )}
                            <Row label="ID transakcji" value={tx.id} mono />
                            <Row label="Status" value={
                                <StatusBadge status={tx.status} />
                            } />
                            <Row label="Data transakcji" value={dateStr} />
                        </div>
                    )}
                </div>

                {/* Stopka */}
                <div className="px-6 pb-5 pt-1">
                    <button
                        id="tx-detail-close"
                        onClick={onClose}
                        className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-[13px] font-medium transition-colors duration-150"
                    >
                        Zamknij
                    </button>
                </div>
            </div>

            <style>{`
                @keyframes fade-in  { from { opacity: 0 } to { opacity: 1 } }
                @keyframes slide-up { from { opacity: 0; transform: translateY(20px) scale(.97) } to { opacity: 1; transform: translateY(0) scale(1) } }
                .animate-fade-in  { animation: fade-in  .15s ease-out both }
                .animate-slide-up { animation: slide-up .2s ease-out both }
            `}</style>
        </div>
    )
}
