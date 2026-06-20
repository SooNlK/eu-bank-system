import { useState, useEffect } from 'react'
import BalanceCard from './BalanceCard'
import TransactionList from './TransactionList'
import TransactionDetailModal from './TransactionDetailModal'
import { getMyAccounts, getAccountTransactions, getTransfers } from '../services/account'

function formatEur(amount) {
    return `€ ${amount.toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export default function DashboardPanel({ onNewTransfer, isJunior }) {
    const [accounts, setAccounts] = useState([])
    const [transactions, setTransactions] = useState([])
    const [loading, setLoading] = useState(true)
    const [mainAccountId, setMainAccountId] = useState(null)
    const [selectedTxId, setSelectedTxId] = useState(null)

    useEffect(() => {
        async function loadData() {
            try {
                const [accs, allTransfers] = await Promise.all([
                    getMyAccounts(),
                    getTransfers().catch(() => []) // Fallback if transfers endpoint fails
                ])
                setAccounts(accs)

                if (accs.length > 0) {
                    const mainAccountId = accs[0].id
                    setMainAccountId(mainAccountId)
                    const trans = await getAccountTransactions(mainAccountId)

                    // Filter transfers that are pending parent approval or rejected
                    const pendingOrRejectedTransfers = allTransfers.filter(
                        t => t.fromAccountId === mainAccountId && (t.status === 'PENDING_APPROVAL' || t.status === 'REJECTED')
                    )

                    // Map transfers to match transaction properties
                    const mappedTransfers = pendingOrRejectedTransfers.map(t => ({
                        id: t.id,
                        accountId: t.fromAccountId,
                        amount: t.amount,
                        currency: t.currency,
                        type: 'DEBIT',
                        status: t.status,
                        description: t.description,
                        createdAt: t.createdAt,
                        isPendingTransfer: true
                    }))

                    // Merge and sort chronologically
                    const mergedTransactions = [...mappedTransfers, ...trans].sort(
                        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
                    )

                    setTransactions(mergedTransactions)
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
        .filter(t => t.type === 'CREDIT' && t.status === 'COMPLETED')
        .reduce((sum, t) => sum + (t.amount?.amount ?? t.amount ?? 0), 0)

    const expenses = monthlyTransactions
        .filter(t => t.type === 'DEBIT' && t.status === 'COMPLETED')
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
        <>
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

                {/* Saldo */}
                <div className="w-full">
                    <BalanceCard accounts={accounts} loading={loading} />
                </div>

                <TransactionList
                    transactions={transactions}
                    loading={loading}
                    onSelect={(txId) => {
                        const tx = transactions.find(t => t.id === txId)
                        if (tx?.isPendingTransfer) {
                            alert(tx.status === 'PENDING_APPROVAL'
                                ? 'Ten przelew czeka na akceptację przez rodzica w jego Strefie Rodzica. 🧸📱'
                                : 'Ten przelew został odrzucony przez rodzica. ❌'
                            )
                            return
                        }
                        setSelectedTxId(txId)
                    }}
                />
            </div>

            {/* Modal szczegółów transakcji */}
            {selectedTxId && mainAccountId && (
                <TransactionDetailModal
                    accountId={mainAccountId}
                    transactionId={selectedTxId}
                    onClose={() => setSelectedTxId(null)}
                />
            )}
        </>
    )
}
