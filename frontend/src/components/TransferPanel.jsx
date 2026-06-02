import { useState, useEffect } from 'react'
import { getMyAccounts, createExternalTransfer } from '../services/account'

const inputClass =
    'w-full text-[12px] border border-slate-200 rounded-[9px] px-3 py-2 text-slate-800 bg-slate-50 outline-none focus:border-blue-400'

const labelClass = 'text-[11px] text-slate-500 font-medium mb-1.5 block'

function formatCurrency(value, currency = 'EUR') {
    return new Intl.NumberFormat('pl-PL', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
    }).format(value)
}

function accountLabel(account) {
    return account.type === 'STANDARD' ? 'Rachunek główny' : account.type
}

function WalletIcon({ color = '#1a3c8f' }) {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <rect x="2" y="7" width="20" height="14" rx="2" stroke={color} strokeWidth="1.8" />
            <path d="M16 14a1 1 0 1 1 0-2 1 1 0 0 1 0 2z" fill={color} />
            <path d="M6 7V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2" stroke={color} strokeWidth="1.8" />
        </svg>
    )
}

function AccountOption({ account, selected, onSelect, disabled }) {
    const color = selected ? '#1a3c8f' : '#64748b'

    return (
        <button
            type="button"
            onClick={() => !disabled && onSelect(account.id)}
            disabled={disabled}
            className={`w-full text-left rounded-[11px] border p-3 flex items-center gap-3 transition-all duration-150
                ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                ${selected
                    ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500/20'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                }`}
        >
            <div
                className="w-9 h-9 rounded-[9px] flex items-center justify-center shrink-0"
                style={{ background: `${color}18` }}
            >
                <WalletIcon color={color} />
            </div>
            <div className="min-w-0 flex-1">
                <p className="text-[12px] font-medium text-slate-800 truncate">{accountLabel(account)}</p>
                <p className="text-[10px] text-slate-400 truncate font-mono">{account.accountNumber}</p>
            </div>
            <div className="text-right shrink-0">
                <p className="text-[13px] font-semibold text-slate-800">
                    {formatCurrency(account.balance, account.currency)}
                </p>
                <p className="text-[10px] text-slate-400">{account.currency}</p>
            </div>
        </button>
    )
}

const CONFIGS = {
    sepa: {
        title: 'Przelew SEPA',
        iconBg: 'bg-blue-50',
        icon: (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="#2563eb" strokeWidth="1.8" />
                <path d="M2 12h20" stroke="#2563eb" strokeWidth="1.8" />
            </svg>
        )
    },
    instant: {
        title: 'Przelew SEPA Instant',
        iconBg: 'bg-green-50',
        icon: (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="#16a34a" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
        )
    },
    target: {
        title: 'Przelew TARGET2',
        iconBg: 'bg-purple-50',
        icon: (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="#9333ea" strokeWidth="1.8" />
                <path d="M3 9h18M9 21V9" stroke="#9333ea" strokeWidth="1.8" />
            </svg>
        )
    }
}

export default function TransferPanel({ onClose, initialType = 'sepa', onDashboardReturn }) {
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
    const [accounts, setAccounts] = useState([])
    const [fromId, setFromId] = useState('')
    const [error, setError] = useState('')
    const [initializing, setInitializing] = useState(true)
    const [loading, setLoading] = useState(false)
    const [transfer, setTransfer] = useState(null)

    useEffect(() => {
        async function loadAccounts() {
            try {
                const nextAccounts = await getMyAccounts()
                setAccounts(nextAccounts)
                setFromId(nextAccounts[0]?.id || '')
            } catch (err) {
                setError(err.message || 'Nie udało się pobrać rachunków')
            } finally {
                setInitializing(false)
            }
        }

        loadAccounts()
    }, [])

    const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

    const fromAccount = accounts.find((a) => a.id === fromId)
    const amountNum = parseFloat(form.amount)
    const isAmountValid = !isNaN(amountNum) && amountNum > 0
    const isOverBalance = Boolean(fromAccount) && isAmountValid && amountNum > fromAccount.balance
    
    // For SEPA Instant, there's a limit of 100 000 EUR
    const isOverLimit = selected === 'instant' && amountNum > 100000

    const canSubmit = Boolean(fromAccount) && isAmountValid && !isOverBalance && !isOverLimit && !loading

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (!canSubmit) return

        setLoading(true)
        setError('')

        // Ustal kanał płatności
        const channelMap = { sepa: 'SEPA', instant: 'SEPA_INSTANT', target: 'TARGET' }
        const channel = channelMap[selected] || 'SEPA'

        try {
            const result = await createExternalTransfer({
                fromAccountId: fromId,
                toIban: form.iban,
                toBic: form.bic || null,
                beneficiaryName: form.beneficiaryName || null,
                amount: amountNum,
                currency: fromAccount.currency || 'EUR',
                channel,
                valueDate: form.executionDate || form.valueDate || null,
                description: form.remittance || null,
            })

            setTransfer({
                id: result.id,
                amount: result.amount ?? amountNum,
                currency: result.currency ?? (fromAccount.currency || 'EUR'),
                status: result.status,
                channel: result.channel,
                externalRef: result.id,
            })

            // Odśwież saldo lokalnie (backend już je zaktualizował)
            if (result.status !== 'PENDING_APPROVAL') {
                fromAccount.balance = Math.max(0, (fromAccount.balance || 0) - amountNum)
            }
        } catch (err) {
            setError(err.message || 'Nie udało się zlecić przelewu')
        } finally {
            setLoading(false)
        }
    }


    if (transfer) {
        const completed = transfer.status === 'COMPLETED'
        const pendingApproval = transfer.status === 'PENDING_APPROVAL'
        const processing = transfer.status === 'PROCESSING' // SEPA Batch – w kolejce

        let statusBg = 'bg-red-50'
        let statusStroke = '#dc2626'
        let statusTitle = 'Przelew nie został wykonany'

        if (completed) {
            statusBg = 'bg-green-50'
            statusStroke = '#16a34a'
            statusTitle = 'Przelew wykonany ✓'
        } else if (pendingApproval) {
            statusBg = 'bg-amber-50'
            statusStroke = '#d97706'
            statusTitle = 'Oczekiwanie na zatwierdzenie 🧸'
        } else if (processing) {
            statusBg = 'bg-blue-50'
            statusStroke = '#2563eb'
            statusTitle = 'Przelew w kolejce SEPA ⏳'
        }

        return (
            <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4 flex flex-col items-center justify-center gap-3 py-10 animate-in fade-in duration-200">
                <div className={`w-14 h-14 ${statusBg} rounded-full flex items-center justify-center mb-1`}>
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="12" r="10" stroke={statusStroke} strokeWidth="1.8" />
                        {completed && (
                            <path d="M7.5 12l3 3 6-6" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        )}
                        {pendingApproval && (
                            <path d="M12 8v4m0 4h.01" stroke="#d97706" strokeWidth="2" strokeLinecap="round" />
                        )}
                        {processing && (
                            <path d="M12 6v6l4 2" stroke="#2563eb" strokeWidth="2" strokeLinecap="round" />
                        )}
                        {!completed && !pendingApproval && !processing && (
                            <path d="M8 8l8 8M16 8l-8 8" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" />
                        )}
                    </svg>
                </div>
                <p className="text-[15px] font-semibold text-slate-800">
                    {statusTitle}
                </p>
                <div className="text-[12px] text-slate-500 text-center max-w-[280px]">
                    {pendingApproval ? (
                        <p className="leading-relaxed">
                            Przelew oczekuje na zatwierdzenie przez rodzica! 🧸📱<br />
                            Kwota: <strong className="text-slate-800">{formatCurrency(transfer.amount, transfer.currency)}</strong>.
                        </p>
                    ) : processing ? (
                        <p className="leading-relaxed">
                            Przelew SEPA przyjęty do rozliczenia.<br />
                            Kwota: <strong className="text-slate-800">{formatCurrency(transfer.amount, transfer.currency)}</strong>.<br />
                            <span className="text-blue-500 font-medium">Rozliczenie nastąpi w ciągu ~5 minut (sesja SEPA Batch).</span>
                        </p>
                    ) : (
                        <p className="leading-relaxed">
                            Przelew został wysłany pomyślnie!<br />
                            Kwota: <strong className="text-slate-800">{formatCurrency(transfer.amount, transfer.currency)}</strong>.
                        </p>
                    )}
                </div>

                <div className="flex gap-3 mt-4 w-full px-4 sm:px-0 sm:w-auto">
                    <button
                        onClick={() => {
                            setForm({
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
                            });
                            setTransfer(null);
                        }}
                        className="flex-1 sm:flex-none bg-slate-100 text-slate-700 border-none rounded-[9px] px-5 py-2.5 sm:py-2 text-[13px] font-medium cursor-pointer hover:bg-slate-200 transition-colors"
                    >
                        Nowy przelew
                    </button>
                    <button
                        onClick={onDashboardReturn || onClose}
                        className={`flex-1 sm:flex-none text-white border-none rounded-[9px] px-5 py-2.5 sm:py-2 text-[13px] font-medium cursor-pointer transition-colors shadow-sm
                            ${pendingApproval ? 'bg-amber-600 hover:bg-amber-700' : 'bg-blue-600 hover:bg-blue-700'}`}
                    >
                        Wróć do pulpitu
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-5 mb-4 animate-in fade-in duration-200">
            <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-2.5">
                    <div className={`w-8 h-8 ${CONFIGS[selected]?.iconBg || 'bg-blue-50'} rounded-lg flex items-center justify-center shrink-0`}>
                        {CONFIGS[selected]?.icon}
                    </div>
                    <p className="text-[14px] font-medium text-slate-800">{CONFIGS[selected]?.title || 'Przelew'}</p>
                </div>
                <button
                    type="button"
                    onClick={onClose}
                    className="bg-transparent border-none cursor-pointer text-slate-400 text-[18px] leading-none hover:text-slate-600 transition-colors"
                    aria-label="Zamknij"
                >
                    ✕
                </button>
            </div>

            {initializing ? (
                <div className="h-44 rounded-xl bg-slate-100 animate-pulse" />
            ) : (
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className={labelClass}>Z rachunku</label>
                        <div className="flex flex-col gap-2">
                            {accounts.map((acc) => (
                                <AccountOption
                                    key={acc.id}
                                    account={acc}
                                    selected={fromId === acc.id}
                                    onSelect={setFromId}
                                    disabled={loading}
                                />
                            ))}
                        </div>
                    </div>

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
                            {isOverBalance && (
                                <p className="text-[11px] text-red-500 mt-1 flex items-center gap-1 font-medium">
                                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
                                        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                                        <path d="M12 8v4m0 4h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                    Kwota przekracza dostępne saldo ({formatCurrency(fromAccount?.balance || 0, fromAccount?.currency)})
                                </p>
                            )}
                            {isOverLimit && (
                                <p className="text-[11px] text-red-500 mt-1 flex items-center gap-1 font-medium">
                                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
                                        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                                        <path d="M12 8v4m0 4h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                    Maksymalny limit transakcji SCT Inst to 100 000 EUR
                                </p>
                            )}
                            {isAmountValid && !isOverBalance && !isOverLimit && (
                                <p className="text-[11px] text-slate-400 mt-1">
                                    Dostępne saldo: {formatCurrency(fromAccount?.balance || 0, fromAccount?.currency || 'EUR')}
                                </p>
                            )}
                            {!isAmountValid && selected === 'instant' && (
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

                    {error && (
                        <p className="text-[12px] text-red-600 bg-red-50 border border-red-100 rounded-[9px] px-3 py-2 font-medium">
                            {error}
                        </p>
                    )}

                    <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 pt-1">
                        <button
                            type="button"
                            onClick={onClose}
                            className="border border-slate-200 rounded-[9px] px-5 py-2 text-[13px] font-medium text-slate-700 bg-white cursor-pointer hover:bg-slate-50 transition-colors"
                        >
                            Anuluj
                        </button>
                        <button
                            type="submit"
                            disabled={!canSubmit}
                            className={`rounded-[9px] px-5 py-2 text-[13px] font-medium border-none transition-all
                                ${canSubmit
                                    ? 'bg-blue-600 text-white cursor-pointer hover:bg-blue-700 shadow-sm'
                                    : 'bg-slate-100 text-slate-400 cursor-not-allowed'
                                }`}
                        >
                            {loading ? (
                                <span className="flex items-center gap-2">
                                    <svg className="animate-spin" width="13" height="13" viewBox="0 0 24 24" fill="none">
                                        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" opacity="0.25" />
                                        <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                    Przetwarzanie...
                                </span>
                            ) : (
                                'Wyślij przelew'
                            )}
                        </button>
                    </div>
                </form>
            )}
        </div>
    )
}
