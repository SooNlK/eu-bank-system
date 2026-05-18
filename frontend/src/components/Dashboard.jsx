import { useState } from 'react'
import Sidebar from './SideBar'
import TopBar from './TopBar'
import DashboardPanel from './DashboardPanel'
import TransfersPanel from './TransfersPanel'

const TRANSFER_NAV_IDS = ['sepa', 'instant', 'target', 'internal', 'transfers']

export default function Dashboard({ userEmail, onLogout }) {
    const [activeNav, setActiveNav] = useState('dashboard')

    const handleNavChange = (id) => {
        // Dla "Przelewy" z głównego menu — ustaw domyślnie 'transfers' (brak pre-selekcji)
        setActiveNav(id)
    }

    const renderContent = () => {

        if (TRANSFER_NAV_IDS.includes(activeNav)) {
            // Jeśli kliknięto konkretny sub-typ (sepa/instant/target/internal), otwórz od razu ten formularz.
            // Jeśli kliknięto ogólne "Przelewy", pokaż widok bez pre-selekcji.
            const initialType = activeNav === 'transfers' ? null : activeNav
            return (
                <TransfersPanel
                    key={activeNav}           // reset stanu formularza przy każdej zmianie nav
                    initialType={initialType}
                    onTypeChange={(id) => setActiveNav(id)}
                />
            )
        }
        // dashboard (default)
        return (
            <DashboardPanel
                onNewTransfer={() => setActiveNav('sepa')}
            />
        )
    }

    return (
        <div className="grid h-screen w-screen grid-cols-[220px_1fr] rounded-none overflow-hidden border border-slate-200/80 bg-[#f0f4ff]">

            <Sidebar activeNav={activeNav} onNavChange={handleNavChange} userEmail={userEmail} />

            <div className="flex flex-col overflow-hidden">
                <TopBar userEmail={userEmail} onLogout={onLogout} />

                <div className="flex-1 overflow-y-auto px-6 py-5">
                    {renderContent()}
                </div>
            </div>
        </div>
    )
}
