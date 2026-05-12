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

const RECENT_TRANSFERS = [
    { name: 'Jan Kowalski', iban: 'PL83 1090 2590 ...', amount: '€ 500,00', date: '21 kwi', type: 'SEPA Instant', positive: true },
    { name: 'Amazon Europe', iban: 'DE89 3704 ...', amount: '€ 89,99', date: '22 kwi', type: 'SEPA', positive: false },
    { name: 'Deutsche Bank AG', iban: 'DE91 1000 ...', amount: '€ 12 000,00', date: '20 kwi', type: 'TARGET2', positive: false },
    { name: 'Konto oszczędnościowe', iban: 'PL83 1090 ...', amount: '€ 2 000,00', date: '15 kwi', type: 'Wewn.', positive: true },
]

export default function TransfersPanel({ initialType = null, onTypeChange }) {
    const [activeForm, setActiveForm] = useState(initialType)

    const handleSelect = (id) => {
        setActiveForm(id)
        if (onTypeChange) onTypeChange(id)
    }

    const handleClose = () => {
        setActiveForm(null)
        if (onTypeChange) onTypeChange('transfers')
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
                {TRANSFER_TYPES.map(({ id, label, subtitle, iconBg, icon, description }) => (
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
                        <InternalTransferPanel onClose={handleClose} />
                    ) : (
                        <TransferPanel initialType={activeForm} onClose={handleClose} />
                    )}
                </div>
            )}

            {/* Ostatnie przelewy */}
            {!activeForm && (
                <div className="bg-white rounded-2xl border border-slate-200/70 p-4">
                    <p className="text-[13px] font-medium text-slate-800 mb-3">Ostatnie przelewy</p>
                    {RECENT_TRANSFERS.map(({ name, iban, amount, date, type, positive }) => (
                        <div key={name + date} className="flex items-center gap-3 py-2.5 border-b border-slate-100 last:border-b-0">
                            <div className="w-9 h-9 bg-slate-100 rounded-[10px] flex items-center justify-center shrink-0">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                                    <path d="M17 20h5v-2a3 3 0 0 0-5.356-1.857M7 20H2v-2a3 3 0 0 1 5.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 0 1 9.288 0"
                                        stroke="#64748b" strokeWidth="1.6" strokeLinecap="round" />
                                </svg>
                            </div>
                            <div className="flex-1 min-w-0">
                                <p className="text-[12px] font-medium text-slate-800 truncate">{name}</p>
                                <div className="flex items-center gap-1.5 mt-px">
                                    <span className="inline-flex items-center text-[10px] font-medium rounded-[5px] px-1.5 py-0.5 bg-slate-100 text-slate-600">
                                        {type}
                                    </span>
                                    <span className="text-[10px] text-slate-400">{date} · {iban}</span>
                                </div>
                            </div>
                            <span className={`text-[13px] font-medium ${positive ? 'text-green-600' : 'text-red-600'}`}>
                                {positive ? '+ ' : '- '}{amount}
                            </span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}
