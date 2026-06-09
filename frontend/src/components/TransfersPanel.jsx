import { useState } from 'react'
import TransferPanel from './TransferPanel'
import InternalTransferPanel from './InternalTransferPanel'

const TRANSFER_TYPES = [
    {
        id: 'sepa',
        label: 'SEPA',
        subtitle: 'SCT · do 1 dnia roboczego',
        iconBg: 'bg-blue-50',
        iconColor: '#2563eb',
        icon: (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="#2563eb" strokeWidth="1.8" />
                <path d="M2 12h20" stroke="#2563eb" strokeWidth="1.8" />
            </svg>
        ),
        description: 'Przelew w strefie SEPA do banku odbiorcy w ciągu 1 dnia roboczego. Limit: brak (SCT).',
    },
    {
        id: 'instant',
        label: 'SEPA Instant',
        subtitle: 'SCT Inst · do 10 sekund',
        iconBg: 'bg-green-50',
        iconColor: '#16a34a',
        icon: (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="#16a34a" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
        ),
        description: 'Błyskawiczny przelew SEPA. Środki u odbiorcy w 10 sekund, 24/7. Limit: 100 000 EUR.',
    },
    {
        id: 'target',
        label: 'TARGET2',
        subtitle: 'RTGS · Eurosystem',
        iconBg: 'bg-purple-50',
        iconColor: '#9333ea',
        icon: (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="#9333ea" strokeWidth="1.8" />
                <path d="M3 9h18M9 21V9" stroke="#9333ea" strokeWidth="1.8" />
            </svg>
        ),
        description: 'Rozliczenie brutto w czasie rzeczywistym przez Eurosystem. Dla dużych kwot międzybankowych.',
    },
    {
        id: 'swift',
        label: 'SWIFT',
        subtitle: 'Przelew walutowy · cały świat',
        iconBg: 'bg-orange-50',
        iconColor: '#ea580c',
        icon: (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="#ea580c" strokeWidth="1.8" />
                <path d="M2 12h20M12 3c-2.5 3-4 5.5-4 9s1.5 6 4 9M12 3c2.5 3 4 5.5 4 9s-1.5 6-4 9" stroke="#ea580c" strokeWidth="1.8" />
            </svg>
        ),
        description: 'Przelew międzynarodowy w walutach obcych (USD, GBP, PLN itp.). Koszt: 1%.',
    },
    {
        id: 'internal',
        label: 'Przelew wewnętrzny',
        subtitle: 'Między własnymi rachunkami',
        iconBg: 'bg-indigo-50',
        iconColor: '#6366f1',
        icon: (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M7 16V4m0 0L3 8m4-4l4 4" stroke="#6366f1" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M17 8v12m0 0l4-4m-4 4l-4-4" stroke="#6366f1" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        ),
        description: 'Natychmiastowe przesunięcie środków między własnymi rachunkami w EuroBank. Bez opłat.',
    },
]

const RECENT_TRANSFERS = []

export default function TransfersPanel({ initialType = null, onTypeChange, isJunior }) {
    const [activeForm, setActiveForm] = useState(isJunior ? 'internal' : initialType)

    const visibleTypes = isJunior 
        ? TRANSFER_TYPES.filter(t => t.id === 'internal')
        : TRANSFER_TYPES

    const handleSelect = (id) => {
        setActiveForm(id)
        if (onTypeChange) onTypeChange(id)
    }

    const handleClose = () => {
        if (isJunior) {
            // A child user closing the form returns to dashboard
            if (onTypeChange) onTypeChange('dashboard')
        } else {
            setActiveForm(null)
            if (onTypeChange) onTypeChange('transfers')
        }
    }

    return (
        <div className="flex flex-col gap-4">
            {/* Tytuł sekcji */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-[16px] font-semibold text-slate-800">Przelewy</h2>
                    <p className="text-[12px] text-slate-400">Wybierz typ przelewu, który chcesz zlecić</p>
                </div>
            </div>

            {/* Kafelki typów przelewów */}
            <div className="grid grid-cols-2 gap-3">
                {visibleTypes.map(({ id, label, subtitle, iconBg, icon, description }) => (
                    <button
                        key={id}
                        type="button"
                        onClick={() => handleSelect(id)}
                        className={`text-left bg-white rounded-2xl border p-4 flex gap-3.5 cursor-pointer transition-all duration-150
                            ${activeForm === id
                                ? 'border-blue-500 ring-1 ring-blue-500/20 bg-blue-50/30'
                                : 'border-slate-200/70 hover:border-slate-300 hover:shadow-sm'
                            }`}
                    >
                        <div className={`w-11 h-11 ${iconBg} rounded-[11px] flex items-center justify-center shrink-0`}>
                            {icon}
                        </div>
                        <div className="min-w-0">
                            <p className="text-[13px] font-semibold text-slate-800">{label}</p>
                            <p className="text-[11px] text-slate-400 mb-1">{subtitle}</p>
                            <p className="text-[10.5px] text-slate-500 leading-snug">{description}</p>
                        </div>
                    </button>
                ))}
            </div>

            {/* Formularz przelewu */}
            {activeForm && (
                <div>
                    {activeForm === 'internal' ? (
                        <InternalTransferPanel onClose={handleClose} onDashboardReturn={() => onTypeChange && onTypeChange('dashboard')} />
                    ) : (
                        <TransferPanel initialType={activeForm} onClose={handleClose} onDashboardReturn={() => onTypeChange && onTypeChange('dashboard')} />
                    )}
                </div>
            )}

        </div>
    )
}
