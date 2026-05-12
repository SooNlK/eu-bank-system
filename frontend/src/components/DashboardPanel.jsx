import BalanceCard from './BalanceCard'
import QuickActions from './QuickActions'
import TransactionList from './TransactionList'

const STATS = [
    {
        label: 'Przychody (kwiecień)',
        value: '€ 3 700,00',
        change: '+12%',
        positive: true,
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 19V5M5 12l7-7 7 7" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        ),
        iconBg: 'bg-green-50',
    },
    {
        label: 'Wydatki (kwiecień)',
        value: '€ 12 233,49',
        change: '-5%',
        positive: false,
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 5v14M5 12l7 7 7-7" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        ),
        iconBg: 'bg-red-50',
    },
    {
        label: 'Przelewy oczekujące',
        value: '2',
        change: null,
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="#2563eb" strokeWidth="1.8" />
                <path d="M12 8v4l2.5 2.5" stroke="#2563eb" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
        ),
        iconBg: 'bg-blue-50',
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
            <div className="grid grid-cols-2 gap-4">
                <TransactionList />
                <div className="bg-white rounded-2xl border border-slate-200/70 p-4">
                    <p className="text-[13px] font-medium text-slate-800 mb-3">Nadchodzące płatności</p>
                    {[
                        { name: 'Czynsz miesięczny', date: '1 maj', amount: '€ 850,00', color: 'text-red-600' },
                        { name: 'Ubezpieczenie OC', date: '5 maj', amount: '€ 120,00', color: 'text-red-600' },
                        { name: 'Subskrypcja SaaS', date: '12 maj', amount: '€ 49,00', color: 'text-red-600' },
                    ].map(({ name, date, amount, color }) => (
                        <div key={name} className="flex items-center gap-3 py-2.5 border-b border-slate-100 last:border-b-0">
                            <div className="w-2 h-2 rounded-full bg-amber-400 shrink-0" />
                            <div className="flex-1 min-w-0">
                                <p className="text-[12px] font-medium text-slate-800 truncate">{name}</p>
                                <p className="text-[10px] text-slate-400">{date}</p>
                            </div>
                            <span className={`text-[13px] font-medium ${color}`}>{amount}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    )
}
