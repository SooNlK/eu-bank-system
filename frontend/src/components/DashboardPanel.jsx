import { useState, useEffect } from 'react'
import BalanceCard from './BalanceCard'
import QuickActions from './QuickActions'
import TransactionList from './TransactionList'
import { getMyAccounts, getAccountTransactions } from '../services/account'

function formatEur(amount) {
    return `€ ${amount.toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export default function DashboardPanel({ onNewTransfer }) {
    const [accounts, setAccounts] = useState([])
    const [transactions, setTransactions] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        async function loadData() {
            try {
                const accs = await getMyAccounts()
                setAccounts(accs)

                if (accs.length > 0) {
                    const mainAccountId = accs[0].id
                    const trans = await getAccountTransactions(mainAccountId)
                    setTransactions(trans)
                }
            } catch (error) {
                console.error("Failed to fetch dashboard data:", error)
            } finally {
                setLoading(false)
            }
        }
        loadData()
    }, [])

    // Oblicz przychody i wydatki bieżącego miesiąca
    const now = new Date()
    const currentMonth = now.getMonth()
    const currentYear = now.getFullYear()

    const monthlyTransactions = transactions.filter(t => {
        const d = new Date(t.createdAt || t.date)
        return d.getMonth() === currentMonth && d.getFullYear() === currentYear
    })

    const income = monthlyTransactions
        .filter(t => t.type === 'CREDIT')
        .reduce((sum, t) => sum + (t.amount?.amount ?? t.amount ?? 0), 0)

    const expenses = monthlyTransactions
        .filter(t => t.type === 'DEBIT')
        .reduce((sum, t) => sum + (t.amount?.amount ?? t.amount ?? 0), 0)

    const monthName = now.toLocaleString('pl-PL', { month: 'long' })

    const STATS = [
        {
            label: `Przychody (${monthName})`,
            value: formatEur(income),
            positive: true,
            icon: (
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M12 19V5M5 12l7-7 7 7" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
            ),
            iconBg: 'bg-green-50',
        },
        {
            label: `Wydatki (${monthName})`,
            value: formatEur(expenses),
            positive: false,
            icon: (
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M12 5v14M5 12l7 7 7-7" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
            ),
            iconBg: 'bg-red-50',
        },
    ]

    return (
        <div className="flex flex-col gap-4">
            {/* Statystyki */}
            <div className="grid grid-cols-3 gap-3">
                {STATS.map(({ label, value, positive, icon, iconBg }) => (
                    <div key={label} className="bg-white rounded-2xl border border-slate-200/70 p-4 flex items-center gap-3">
                        <div className={`w-10 h-10 ${iconBg} rounded-[10px] flex items-center justify-center shrink-0`}>
                            {icon}
                        </div>
                        <div>
                            <p className="text-[11px] text-slate-500">{label}</p>
                            <p className={`text-[15px] font-semibold ${positive ? 'text-green-700' : 'text-red-600'}`}>{value}</p>
                        </div>
                    </div>
                ))}
            </div>

            {/* Saldo + szybkie akcje */}
            <div className="grid grid-cols-[2fr_1fr] gap-4">
                <BalanceCard accounts={accounts} loading={loading} />
                <QuickActions onNewTransfer={onNewTransfer} />
            </div>

            {/* Ostatnie transakcje */}
            <TransactionList transactions={transactions} loading={loading} />
        </div>
    )
}
