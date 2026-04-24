import { useState } from 'react'

const TRANSFER_TYPES = [
    {
        id: 'sepa',
        label: 'SEPA',
        subtitle: '1 dzień roboczy',
        iconBg: 'bg-blue-50',
        icon: (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="#2563eb" strokeWidth="1.8" />
                <path d="M2 12h20" stroke="#2563eb" strokeWidth="1.8" />
            </svg>
        ),
    },
    {
        id: 'instant',
        label: 'SEPA Instant',
        subtitle: 'do 10 sekund',
        iconBg: 'bg-green-50',
        icon: (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="#16a34a" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
        ),
    },
    {
        id: 'target',
        label: 'TARGET2',
        subtitle: 'Rozliczenia EBC',
        iconBg: 'bg-purple-50',
        icon: (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="#9333ea" strokeWidth="1.8" />
                <path d="M3 9h18M9 21V9" stroke="#9333ea" strokeWidth="1.8" />
            </svg>
        ),
    },
]

export default function TransferPanel({ onClose }) {
    const [selected, setSelected] = useState('sepa')

    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4">
            <div className="flex items-center justify-between mb-4">
                <p className="text-[14px] font-medium text-slate-800">Nowy przelew</p>
                <button
                    onClick={onClose}
                    className="bg-transparent border-none cursor-pointer text-slate-400 text-[18px] leading-none"
                >
                    ✕
                </button>
            </div>

            <div className="grid grid-cols-3 gap-2.5 mb-4">
                {TRANSFER_TYPES.map(({ id, label, subtitle, iconBg, icon }) => (
                    <button
                        key={id}
                        onClick={() => setSelected(id)}
                        className={`border rounded-xl p-3 cursor-pointer flex items-center gap-3 text-left w-full transition-colors
                            ${selected === id
                                ? 'border-blue-500 bg-blue-50'
                                : 'border-slate-200 bg-white hover:border-slate-300'
                            }`}
                    >
                        <div className={`w-8 h-8 ${iconBg} rounded-lg flex items-center justify-center shrink-0`}>
                            {icon}
                        </div>
                        <div>
                            <p className="text-[12px] font-medium text-slate-800">{label}</p>
                            <p className="text-[10px] text-slate-500">{subtitle}</p>
                        </div>
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-[1fr_1fr_auto] gap-2.5 items-end">
                <div>
                    <p className="text-[11px] text-slate-500 font-medium mb-1.5">Odbiorca / IBAN</p>
                    <input
                        type="text"
                        placeholder="DE89 3704 0044 0532 0130 00"
                        className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400"
                    />
                </div>
                <div>
                    <p className="text-[11px] text-slate-500 font-medium mb-1.5">Kwota (EUR)</p>
                    <input
                        type="number"
                        placeholder="0,00"
                        className="w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400"
                    />
                </div>
                <button className="bg-blue-600 text-white border-none rounded-[9px] px-5 py-2 text-[13px] font-medium cursor-pointer hover:bg-blue-700 transition-colors">
                    Wyślij
                </button>
            </div>
        </div>
    )
}
