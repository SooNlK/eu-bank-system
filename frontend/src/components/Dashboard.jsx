import { useState, useEffect, useRef } from 'react'
import Sidebar from './SideBar'
import TopBar from './TopBar'
import DashboardPanel from './DashboardPanel'
import TransfersPanel from './TransfersPanel'
import JuniorPanel from './JuniorPanel'
import CardsPanel from './CardsPanel'
import KlikPanel from './KlikPanel'
import { getMyAccounts, getPendingApprovals } from '../services/account'

const TRANSFER_NAV_IDS = ['sepa', 'instant', 'target', 'internal', 'swift', 'transfers']

export default function Dashboard({ userEmail, onLogout }) {
    const [activeNav, setActiveNav] = useState('dashboard')
    const [isJunior, setIsJunior] = useState(false)
    const [pendingCount, setPendingCount] = useState(0)
    const [toast, setToast] = useState(null)
    const previousPendingIds = useRef(new Set())

    useEffect(() => {
        let isMounted = true

        async function initDashboard() {
            try {
                const accs = await getMyAccounts()
                if (!isMounted) return
                const juniorUser = accs.length > 0 && accs.every(a => a.type === 'JUNIOR')
                setIsJunior(juniorUser)

                // If not junior, load initial pending approvals
                if (!juniorUser) {
                    const approvals = await getPendingApprovals()
                    if (!isMounted) return
                    setPendingCount(approvals.length)
                    previousPendingIds.current = new Set(approvals.map(t => t.id))
                }
            } catch (err) {
                console.error('Failed to initialize dashboard:', err)
            }
        }

        initDashboard()
        return () => { isMounted = false }
    }, [])

    // Setup polling for pending approvals (only for parent/adult)
    useEffect(() => {
        if (isJunior) return

        let intervalId = setInterval(async () => {
            try {
                const approvals = await getPendingApprovals()
                setPendingCount(approvals.length)

                // Check for new approvals to trigger toast
                const currentIds = approvals.map(t => t.id)
                const hasNew = currentIds.some(id => !previousPendingIds.current.has(id))
                
                if (hasNew && approvals.length > 0) {
                    // Show beautiful toast
                    setToast({
                        message: `Nowy przelew od Twojego dziecka (${approvals[approvals.length - 1].fromAccountOwner || 'Junior'}) wymaga akceptacji! 📱`
                    })
                    // Auto-hide after 5 seconds
                    setTimeout(() => setToast(null), 5000)
                }

                previousPendingIds.current = new Set(currentIds)
            } catch (err) {
                console.error('Failed to poll pending approvals:', err)
            }
        }, 5000)

        return () => clearInterval(intervalId)
    }, [isJunior])

    const handleNavChange = (id) => {
        setActiveNav(id)
    }

    const renderContent = () => {
        if (activeNav === 'junior') {
            return <JuniorPanel />
        }

        if (activeNav === 'cards') {
            return <CardsPanel />
        }

        if (activeNav === 'klik') {
            return <KlikPanel />
        }

        if (TRANSFER_NAV_IDS.includes(activeNav)) {
            const initialType = activeNav === 'transfers' ? null : activeNav
            return (
                <TransfersPanel
                    key={activeNav}
                    initialType={initialType}
                    onTypeChange={(id) => setActiveNav(id)}
                    isJunior={isJunior}
                />
            )
        }

        return (
            <DashboardPanel
                onNewTransfer={() => setActiveNav(isJunior ? 'internal' : 'sepa')}
                isJunior={isJunior}
            />
        )
    }

    return (
        <div className="grid h-screen w-screen grid-cols-[220px_1fr] rounded-none overflow-hidden border border-slate-200/80 bg-[#f0f4ff]">

            <Sidebar 
                activeNav={activeNav} 
                onNavChange={handleNavChange} 
                userEmail={userEmail} 
                isJunior={isJunior}
                pendingCount={pendingCount}
            />

            <div className="flex flex-col overflow-hidden">
                <TopBar userEmail={userEmail} onLogout={onLogout} />

                <div className="flex-1 overflow-y-auto px-6 py-5">
                    {renderContent()}
                </div>
            </div>

            {/* Floating Toast Notification */}
            {toast && (
                <div className="fixed bottom-5 right-5 bg-slate-900/90 text-white rounded-2xl border border-slate-700/50 p-4 shadow-2xl z-50 flex items-center gap-3.5 animate-in slide-in-from-bottom duration-300 max-w-sm backdrop-blur-md">
                    <span className="text-[22px] animate-bounce">🔔</span>
                    <div className="flex-1 min-w-0">
                        <p className="text-[12px] font-bold text-white">EuroBank Junior</p>
                        <p className="text-[11px] text-slate-300 leading-snug">{toast.message}</p>
                    </div>
                    <button 
                        onClick={() => setToast(null)} 
                        className="text-white/60 hover:text-white border-none bg-transparent cursor-pointer text-[16px] font-bold px-1"
                    >
                        ×
                    </button>
                </div>
            )}
        </div>
    )
}
