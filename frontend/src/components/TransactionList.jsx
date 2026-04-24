const TRANSACTIONS = [
    {
        id: 1,
        iconBg: 'bg-blue-50',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.3 2.3A1 1 0 0 0 5.4 17H19" stroke="#2563eb" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
        ),
        name: 'Amazon Europe',
        badge: { label: 'SEPA', className: 'bg-blue-50 text-blue-700' },
        date: '22 kwi',
        amount: '- €89,99',
        amountColor: 'text-red-600',
    },
    {
        id: 2,
        iconBg: 'bg-green-50',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M17 20h5v-2a3 3 0 0 0-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 0 1 5.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 0 1 9.288 0" stroke="#16a34a" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
        ),
        name: 'Jan Kowalski',
        badge: { label: 'SEPA Instant', className: 'bg-green-50 text-green-800' },
        date: '21 kwi',
        amount: '+ €500,00',
        amountColor: 'text-green-600',
    },
    {
        id: 3,
        iconBg: 'bg-purple-50',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="#9333ea" strokeWidth="1.6" />
                <path d="M8 12h8M10 9l-2 3 2 3" stroke="#9333ea" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
        ),
        name: 'Deutsche Bank AG',
        badge: { label: 'TARGET2', className: 'bg-purple-50 text-purple-700' },
        date: '20 kwi',
        amount: '- €12 000',
        amountColor: 'text-red-600',
    },
    {
        id: 4,
        iconBg: 'bg-amber-50',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" stroke="#d97706" strokeWidth="1.6" />
                <line x1="3" y1="6" x2="21" y2="6" stroke="#d97706" strokeWidth="1.6" />
            </svg>
        ),
        name: 'Kaufland Polska',
        badge: { label: 'SEPA', className: 'bg-blue-50 text-blue-700' },
        date: '19 kwi',
        amount: '- €143,50',
        amountColor: 'text-red-600',
    },
    {
        id: 5,
        iconBg: 'bg-green-50',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" stroke="#16a34a" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
        ),
        name: 'Wpłata — przelew przych.',
        badge: { label: 'SEPA Instant', className: 'bg-green-50 text-green-800' },
        date: '18 kwi',
        amount: '+ €3 200,00',
        amountColor: 'text-green-600',
    },
]

export default function TransactionList() {
    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-4">
            <div className="flex items-center justify-between mb-3.5">
                <p className="text-[13px] font-medium text-slate-800">Ostatnie transakcje</p>
                <span className="text-[11px] text-blue-600 cursor-pointer">Wszystkie</span>
            </div>

            {TRANSACTIONS.map(({ id, iconBg, icon, name, badge, date, amount, amountColor }) => (
                <div
                    key={id}
                    className="flex items-center gap-3 py-2.5 border-b border-slate-100 last:border-b-0"
                >
                    <div className={`w-9 h-9 ${iconBg} rounded-[10px] flex items-center justify-center shrink-0`}>
                        {icon}
                    </div>
                    <div className="flex-1 min-w-0">
                        <p className="text-[12px] font-medium text-slate-800">{name}</p>
                        <div className="flex items-center gap-1.5 mt-px">
                            <span className={`inline-flex items-center text-[10px] font-medium rounded-[5px] px-1.5 py-0.5 ${badge.className}`}>
                                {badge.label}
                            </span>
                            <span className="text-[10px] text-slate-400">{date}</span>
                        </div>
                    </div>
                    <span className={`text-[13px] font-medium ${amountColor}`}>{amount}</span>
                </div>
            ))}
        </div>
    )
}
