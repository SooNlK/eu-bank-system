import { useState } from 'react'

const inputClass =
    'w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400'

const labelClass = 'text-[11px] text-slate-500 font-medium mb-1.5 block'

export default function TransferPanel({ onClose, initialType = 'sepa' }) {
    const selected = initialType
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

            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="sm:col-span-2">
                        <label className={labelClass}>Nazwa odbiorcy</label>
                        <input
                            type="text"
                            value={form.beneficiaryName}
                            onChange={set('beneficiaryName')}
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
