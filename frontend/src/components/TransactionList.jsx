import { useState } from 'react';

export default function TransactionList({ transactions = [], loading, onSelect }) {
    const [showAll, setShowAll] = useState(false);

    if (loading) {
        return (
            <div className="bg-white rounded-2xl border border-slate-200/70 p-4">
                <div className="flex items-center justify-between mb-3.5">
                    <p className="text-[13px] font-medium text-slate-800">Ostatnie transakcje</p>
                </div>
                <div className="animate-pulse flex flex-col gap-3 mt-4">
                    <div className="h-10 bg-slate-100 rounded" />
                    <div className="h-10 bg-slate-100 rounded" />
                    <div className="h-10 bg-slate-100 rounded" />
                </div>
            </div>
        )
    }

    const mappedTransactions = transactions.map(tx => {
        const isExpense = tx.type === 'DEBIT';
        const rawAmount = tx.amount?.amount ?? tx.amount ?? 0;
        const currency = tx.amount?.currency ?? tx.currency ?? 'EUR';
        const amountStr = new Intl.NumberFormat('pl-PL', { style: 'currency', currency }).format(rawAmount);

        const dateObj = new Date(tx.createdAt);
        const dateStr = dateObj.toLocaleDateString('pl-PL', { day: 'numeric', month: 'short', year: 'numeric' });

        const iconBg = isExpense ? 'bg-red-50' : 'bg-green-50';
        const icon = isExpense ? (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M5 12h14M12 5l7 7-7 7" stroke="#ef4444" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        ) : (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M19 12H5M12 19l-7-7 7-7" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        );

        return {
            id: tx.id,
            iconBg,
            icon,
            name: tx.description || tx.type,
            badge: {
                label: tx.type,
                className: isExpense ? 'bg-red-50 text-red-500' : 'bg-green-50 text-green-600'
            },
            date: dateStr,
            amount: (isExpense ? '- ' : '+ ') + amountStr,
            amountColor: isExpense ? 'text-red-500' : 'text-green-600'
        };
    });

    const transactionsToShow = showAll ? mappedTransactions : mappedTransactions.slice(0, 4);

    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-4">
            <div className="flex items-center justify-between mb-3.5">
                <p className="text-[13px] font-medium text-slate-800">Ostatnie transakcje</p>
                {mappedTransactions.length > 4 && (
                    <button 
                        onClick={() => setShowAll(!showAll)}
                        className="text-[11px] font-medium text-blue-600 hover:text-blue-700 bg-transparent border-none cursor-pointer p-0"
                    >
                        {showAll ? 'Zwiń' : 'Wszystkie'}
                    </button>
                )}
            </div>

            {transactionsToShow.length > 0 ? (
                transactionsToShow.map(({ id, iconBg, icon, name, badge, date, amount, amountColor }) => (
                    <div
                        key={id}
                        onClick={() => onSelect?.(id)}
                        className="flex items-center gap-3 py-2.5 border-b border-slate-100 last:border-b-0 cursor-pointer rounded-xl px-2 -mx-2 hover:bg-slate-50 transition-colors duration-100 group"
                    >
                        <div className={`w-9 h-9 ${iconBg} rounded-[10px] flex items-center justify-center shrink-0`}>
                            {icon}
                        </div>
                        <div className="flex-1 min-w-0">
                            <p className="text-[12px] font-medium text-slate-800 truncate group-hover:text-blue-600 transition-colors">{name}</p>
                            <div className="flex items-center gap-1.5 mt-px">
                                <span className={`inline-flex items-center text-[10px] font-medium rounded-[5px] px-1.5 py-0.5 ${badge.className}`}>
                                    {badge.label}
                                </span>
                                <span className="text-[10px] text-slate-400">{date}</span>
                            </div>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <span className={`text-[13px] font-medium ${amountColor}`}>{amount}</span>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="text-slate-300 group-hover:text-slate-400 transition-colors">
                                <path d="M9 18l6-6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                        </div>
                    </div>
                ))
            ) : (
                <p className="text-[12px] text-slate-400 py-6 text-center">Brak transakcji do wyświetlenia</p>
            )}
        </div>
    )
}
