const TRANSACTIONS = []

export default function TransactionList() {
    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-4">
            <div className="flex items-center justify-between mb-3.5">
                <p className="text-[13px] font-medium text-slate-800">Ostatnie transakcje</p>
                <span className="text-[11px] text-blue-600 cursor-pointer">Wszystkie</span>
            </div>

            {TRANSACTIONS.length > 0 ? (
                TRANSACTIONS.map(({ id, iconBg, icon, name, badge, date, amount, amountColor }) => (
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
                ))
            ) : (
                <p className="text-[12px] text-slate-400 py-6 text-center">Brak transakcji do wyświetlenia</p>
            )}
        </div>
    )
}
