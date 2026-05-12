import { useState } from 'react'
import Sidebar from './SideBar'
import TopBar from './TopBar'
import BalanceCard from './BalanceCard'
import QuickActions from './QuickActions'
import TransferPanel from './TransferPanel'
import InternalTransferPanel from './InternalTransferPanel'
import TransactionList from './TransactionList'

export default function Dashboard() {
    const [activeNav, setActiveNav] = useState('dashboard')
    const [showTransfer, setShowTransfer] = useState(false)
    const [transferType, setTransferType] = useState('sepa')

    const TRANSFER_NAV_IDS = ['sepa', 'instant', 'target', 'internal']

    const handleNavChange = (id) => {
        setActiveNav(id)
        if (TRANSFER_NAV_IDS.includes(id)) {
            setTransferType(id)
            setShowTransfer(true)
        } else {
            setShowTransfer(false)
        }
    }

    return (
        <div className="grid h-screen w-screen grid-cols-[220px_1fr] rounded-[20px] rounded-none overflow-hidden border border-slate-200/80 bg-[#f0f4ff]">

            <Sidebar activeNav={activeNav} onNavChange={handleNavChange} />

            <div className="flex flex-col overflow-hidden">
                <TopBar />

                <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-4">

                    <div className="grid grid-cols-[2fr_1fr] gap-4">
                        <BalanceCard />
                        <QuickActions onNewTransfer={() => { setTransferType('sepa'); setShowTransfer(v => !v) }} />
                    </div>

                    {showTransfer && (
                        transferType === 'internal' ? (
                            <InternalTransferPanel
                                onClose={() => {
                                    setShowTransfer(false)
                                    setActiveNav('dashboard')
                                }}
                            />
                        ) : (
                            <TransferPanel
                                initialType={transferType}
                                onClose={() => {
                                    setShowTransfer(false)
                                    setActiveNav('dashboard')
                                }}
                            />
                        )
                    )}

                    <div className="grid grid-cols-2 gap-4">
                        <TransactionList />
                        <div className="flex flex-col gap-3">
                        </div>
                    </div>

                </div>
            </div>
        </div>
    )
}
