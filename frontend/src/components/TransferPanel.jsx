import { useState } from 'react'

const TRANSFER_TYPES = [
    {
        id: 'sepa',
        label: 'SEPA',
        subtitle: 'SCT · do 1 dnia roboczego',
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
        subtitle: 'SCT Inst · do 10 s',
        iconBg: 'bg-green-50',
        icon: (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="#16a34a" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
        ),
    },
    {
        id: 'target',
        label: 'TARGET (T2)',
        subtitle: 'RTGS · Eurosystem',
        iconBg: 'bg-purple-50',
        icon: (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="#9333ea" strokeWidth="1.8" />
                <path d="M3 9h18M9 21V9" stroke="#9333ea" strokeWidth="1.8" />
            </svg>
        ),
    },
]

const inputClass =
    'w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400'

const labelClass = 'text-[11px] text-slate-500 font-medium mb-1.5 block'

export default function TransferPanel({ onClose, initialType = 'sepa' }) {
    const [selected, setSelected] = useState(initialType)
    const [form, setForm] = useState({
        beneficiaryName: '',
        iban: '',
        bic: '',
        amount: '',
        remittance: '',
        creditorReference: '',
        executionDate: '',
        valueDate: '',
        uetr: '',
        endToEndId: '',
    })

    const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

    const handleSubmit = (e) => {
        e.preventDefault()
    }

    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4">
            <div className="flex items-center justify-between mb-4">
                <p className="text-[14px] font-medium text-slate-800">Nowy przelew</p>
                <button
                    type="button"
                    onClick={onClose}
                    className="bg-transparent border-none cursor-pointer text-slate-400 text-[18px] leading-none hover:text-slate-600"
                    aria-label="Zamknij"
                >
                    ✕
                </button>
            </div>

            <div className="grid grid-cols-3 gap-2.5 mb-5">
                {TRANSFER_TYPES.map(({ id, label, subtitle, iconBg, icon }) => (
                    <button
                        key={id}
                        type="button"
                        onClick={() => setSelected(id)}
                        className={`border rounded-xl p-3 cursor-pointer flex items-center gap-3 text-left w-full transition-colors
                            ${selected === id
                                ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500/20'
                                : 'border-slate-200 bg-white hover:border-slate-300'
                            }`}
                    >
                        <div className={`w-8 h-8 ${iconBg} rounded-lg flex items-center justify-center shrink-0`}>
                            {icon}
                        </div>
                        <div className="min-w-0">
                            <p className="text-[12px] font-medium text-slate-800 truncate">{label}</p>
                            <p className="text-[10px] text-slate-500 leading-tight">{subtitle}</p>
                        </div>
                    </button>
                ))}
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="sm:col-span-2">
                        <label className={labelClass}>Nazwa odbiorcy</label>
                        <input
                            type="text"
                            value={form.beneficiaryName}
                            onChange={set('beneficiaryName')}
                            placeholder="Jan Kowalski lub Nazwa firmy Sp. z o.o."
                            className={inputClass}
                            autoComplete="name"
                            required
                        />
                    </div>
                    <div>
                        <label className={labelClass}>IBAN odbiorcy</label>
                        <input
                            type="text"
                            value={form.iban}
                            onChange={set('iban')}
                            placeholder="DE89 3704 0044 0532 0130 00"
                            className={inputClass}
                            autoComplete="off"
                            required
                        />
                    </div>
                    <div>
                        <label className={labelClass}>
                            BIC / SWIFT banku odbiorcy
                            {selected === 'target' ? (
                                <span className="text-red-500 font-normal"> *</span>
                            ) : (
                                <span className="text-slate-400 font-normal"> (zalecane)</span>
                            )}
                        </label>
                        <input
                            type="text"
                            value={form.bic}
                            onChange={set('bic')}
                            placeholder="COBADEFFXXX"
                            className={inputClass}
                            required={selected === 'target'}
                        />
                    </div>
                    <div>
                        <label className={labelClass}>Kwota (EUR)</label>
                        <input
                            type="number"
                            value={form.amount}
                            onChange={set('amount')}
                            placeholder="0.00"
                            min="0.01"
                            step="0.01"
                            className={inputClass}
                            required
                        />
                        {selected === 'instant' && (
                            <p className="text-[10px] text-slate-400 mt-1">Limit pojedynczej transakcji SCT Inst: 100 000 EUR</p>
                        )}
                    </div>

                    {selected === 'sepa' && (
                        <div>
                            <label className={labelClass}>Preferowana data realizacji</label>
                            <input
                                type="date"
                                value={form.executionDate}
                                onChange={set('executionDate')}
                                className={inputClass}
                            />
                            <p className="text-[10px] text-slate-400 mt-1">Puste = najwcześniejsza możliwa data robocza</p>
                        </div>
                    )}

                    {selected === 'instant' && (
                        <div>
                            <label className={labelClass}>Identyfikator końcowy (opcjonalnie)</label>
                            <input
                                type="text"
                                value={form.endToEndId}
                                onChange={set('endToEndId')}
                                placeholder="np. faktura FV/2025/03 — max 140 znaków wg ISO 20022"
                                maxLength={140}
                                className={inputClass}
                            />
                        </div>
                    )}

                    {selected === 'target' && (
                        <div>
                            <label className={labelClass}>Data waluty</label>
                            <input type="date" value={form.valueDate} onChange={set('valueDate')} className={inputClass} required />
                        </div>
                    )}
                </div>

                {selected === 'target' && (
                    <div>
                        <label className={labelClass}>UETR (opcjonalnie)</label>
                        <input
                            type="text"
                            value={form.uetr}
                            onChange={set('uetr')}
                            placeholder="Format UUID wg ISO 20022 — często nadawany przez bank"
                            className={inputClass}
                        />
                    </div>
                )}

                <div>
                    <label className={labelClass}>Tytuł / opis dla odbiorcy</label>
                    <input
                        type="text"
                        value={form.remittance}
                        onChange={set('remittance')}
                        placeholder="Np. opłata za usługi marzec 2025"
                        maxLength={140}
                        className={inputClass}
                    />
                </div>

                <div>
                    <label className={labelClass}>
                        Referencja strukturalna dla odbiorcy <span className="text-slate-400 font-normal">(opcjonalnie)</span>
                    </label>
                    <input
                        type="text"
                        value={form.creditorReference}
                        onChange={set('creditorReference')}
                        placeholder="np. RF18539007547034 (ISO 11649)"
                        className={inputClass}
                    />
                </div>

                <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 pt-1">
                    <button
                        type="button"
                        onClick={onClose}
                        className="border border-slate-200 rounded-[9px] px-5 py-2 text-[13px] font-medium text-slate-700 bg-white cursor-pointer hover:bg-slate-50"
                    >
                        Anuluj
                    </button>
                    <button
                        type="submit"
                        className="bg-blue-600 text-white border-none rounded-[9px] px-5 py-2 text-[13px] font-medium cursor-pointer hover:bg-blue-700"
                    >
                        Wyślij przelew
                    </button>
                </div>
            </form>
        </div>
    )
}
