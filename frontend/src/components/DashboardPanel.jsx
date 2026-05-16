import BalanceCard from './BalanceCard'
import QuickActions from './QuickActions'
import TransactionList from './TransactionList'

const STATS = [
    {
        label: 'Przychody (maj)',
        value: '€ 0,00',
        change: null,
        positive: true,
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 19V5M5 12l7-7 7 7" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        ),
        iconBg: 'bg-green-50',
    },
    {
        label: 'Wydatki (maj)',
        value: '€ 0,00',
        change: null,
        positive: false,
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 5v14M5 12l7 7 7-7" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        ),
        iconBg: 'bg-red-50',
    },
]

export default function DashboardPanel({ onNewTransfer }) {
    return (
        <div className="flex flex-col gap-4">
            {/* Statystyki */}
            <div className="grid grid-cols-3 gap-3">
                {STATS.map(({ label, value, change, positive, icon, iconBg }) => (
                    <div key={label} className="bg-white rounded-2xl border border-slate-200/70 p-4 flex items-center gap-3">
                        <div className={`w-10 h-10 ${iconBg} rounded-[10px] flex items-center justify-center shrink-0`}>
                            {icon}
                        </div>
                        <div>
                            <p className="text-[11px] text-slate-500">{label}</p>
                            <p className="text-[15px] font-semibold text-slate-800">{value}</p>
                            {change && (
                                <p className={`text-[10px] font-medium ${positive ? 'text-green-600' : 'text-red-500'}`}>
                                    {change} vs. poprz. miesiąc
                                </p>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            {/* Saldo + szybkie akcje */}
            <div className="grid grid-cols-[2fr_1fr] gap-4">
                <BalanceCard />
                <QuickActions onNewTransfer={onNewTransfer} />
            </div>

            {/* Ostatnie transakcje */}
            <TransactionList />
        </div>
    )
}
