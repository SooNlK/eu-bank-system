import { useEffect, useMemo, useState } from 'react'
import { activateCard, blockCard, getCards, getMyAccounts, issueCard, unblockCard } from '../services/account'

const CARD_TYPES = [
    { value: 'VIRTUAL', label: 'Wirtualna' },
    { value: 'PHYSICAL', label: 'Fizyczna' },
    { value: 'PREPAID', label: 'Prepaid' },
]

function formatEur(amount) {
    const value = Number(amount ?? 0)
    return `€ ${value.toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function statusLabel(status) {
    return {
        REQUESTED: 'Zamówiona',
        PRODUCING: 'W produkcji',
        SHIPPED: 'Wysłana',
        ACTIVE: 'Aktywna',
        BLOCKED: 'Zablokowana',
        EXPIRED: 'Wygasła',
        CANCELLED: 'Anulowana',
    }[status] || status
}

function statusClass(status) {
    if (status === 'ACTIVE') return 'bg-emerald-50 text-emerald-700 border-emerald-100'
    if (status === 'BLOCKED') return 'bg-red-50 text-red-700 border-red-100'
    if (status === 'REQUESTED' || status === 'PRODUCING' || status === 'SHIPPED') return 'bg-amber-50 text-amber-700 border-amber-100'
    return 'bg-slate-50 text-slate-600 border-slate-100'
}

export default function CardsPanel() {
    const [accounts, setAccounts] = useState([])
    const [cards, setCards] = useState([])
    const [loading, setLoading] = useState(true)
    const [submitting, setSubmitting] = useState(false)
    const [message, setMessage] = useState(null)
    const [sensitiveCard, setSensitiveCard] = useState(null)
    const [form, setForm] = useState({
        accountId: '',
        cardType: 'VIRTUAL',
        initialBalance: '0',
        dailyLimit: '1000',
        monthlyLimit: '5000',
    })

    useEffect(() => {
        loadData()
    }, [])

    async function loadData() {
        setLoading(true)
        try {
            const [accs, cardList] = await Promise.all([getMyAccounts(), getCards()])
            setAccounts(accs)
            setCards(cardList)
            setForm(current => ({
                ...current,
                accountId: current.accountId || accs[0]?.id || '',
            }))
        } catch (error) {
            setMessage({ type: 'error', text: error.message })
        } finally {
            setLoading(false)
        }
    }

    const selectedAccount = useMemo(
        () => accounts.find(account => account.id === form.accountId),
        [accounts, form.accountId]
    )

    async function handleIssue(event) {
        event.preventDefault()
        setSubmitting(true)
        setMessage(null)
        setSensitiveCard(null)

        try {
            const result = await issueCard({
                accountId: form.accountId,
                cardType: form.cardType,
                initialBalance: Number(form.initialBalance || 0),
                dailyLimit: Number(form.dailyLimit || 0),
                monthlyLimit: Number(form.monthlyLimit || 0),
            })
            setSensitiveCard(result)
            setMessage({ type: 'success', text: 'Karta została zamówiona w sieci kartowej.' })
            await loadData()
        } catch (error) {
            setMessage({ type: 'error', text: error.message })
        } finally {
            setSubmitting(false)
        }
    }

    async function runCardAction(action, cardId) {
        setMessage(null)
        try {
            await action(cardId)
            await loadData()
            setMessage({ type: 'success', text: 'Status karty został zaktualizowany.' })
        } catch (error) {
            setMessage({ type: 'error', text: error.message })
        }
    }

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-[22px] font-semibold text-slate-900">Karty płatnicze</h1>
                    <p className="text-[12px] text-slate-500 mt-1">Wydawanie i obsługa kart przez zewnętrzną sieć kartową.</p>
                </div>
                <button
                    onClick={loadData}
                    className="h-9 px-3 rounded-lg border border-slate-200 bg-white text-[12px] font-medium text-slate-700 hover:bg-slate-50"
                >
                    Odśwież
                </button>
            </div>

            {message && (
                <div className={`rounded-lg border px-4 py-3 text-[12px] ${message.type === 'error' ? 'bg-red-50 border-red-100 text-red-700' : 'bg-emerald-50 border-emerald-100 text-emerald-700'}`}>
                    {message.text}
                </div>
            )}

            {sensitiveCard && (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <p className="text-[12px] font-semibold text-amber-900">Dane karty widoczne tylko raz</p>
                            <p className="text-[11px] text-amber-800 mt-1">Zapisz je teraz, bo sieć kartowa nie zwróci ponownie pełnego PAN i CVV.</p>
                        </div>
                        <button
                            onClick={() => setSensitiveCard(null)}
                            className="border-none bg-transparent text-amber-900 text-[16px] leading-none cursor-pointer"
                        >
                            ×
                        </button>
                    </div>
                    <div className="grid grid-cols-3 gap-3 mt-4">
                        <SensitiveValue label="PAN" value={sensitiveCard.fullPan} />
                        <SensitiveValue label="CVV" value={sensitiveCard.cvv} />
                        <SensitiveValue label="Token" value={sensitiveCard.card?.externalCardToken} />
                    </div>
                </div>
            )}

            <div className="grid grid-cols-[360px_1fr] gap-4 items-start">
                <form onSubmit={handleIssue} className="bg-white rounded-lg border border-slate-200 p-4 flex flex-col gap-3">
                    <h2 className="text-[15px] font-semibold text-slate-900">Nowa karta</h2>

                    <label className="flex flex-col gap-1.5">
                        <span className="text-[11px] font-medium text-slate-500">Rachunek</span>
                        <select
                            value={form.accountId}
                            onChange={event => setForm({ ...form, accountId: event.target.value })}
                            className="h-10 rounded-lg border border-slate-200 px-3 text-[12px] bg-white"
                            required
                        >
                            {accounts.map(account => (
                                <option key={account.id} value={account.id}>
                                    {account.ownerName || account.accountNumber} · {formatEur(account.balance)}
                                </option>
                            ))}
                        </select>
                    </label>

                    <div>
                        <span className="text-[11px] font-medium text-slate-500">Typ karty</span>
                        <div className="grid grid-cols-3 gap-1 mt-1.5 rounded-lg bg-slate-100 p-1">
                            {CARD_TYPES.map(type => (
                                <button
                                    key={type.value}
                                    type="button"
                                    onClick={() => setForm({ ...form, cardType: type.value })}
                                    className={`h-8 rounded-md border-none text-[11px] font-medium cursor-pointer ${form.cardType === type.value ? 'bg-white text-blue-700 shadow-sm' : 'bg-transparent text-slate-500'}`}
                                >
                                    {type.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <MoneyInput
                            label="Limit dzienny"
                            value={form.dailyLimit}
                            onChange={value => setForm({ ...form, dailyLimit: value })}
                        />
                        <MoneyInput
                            label="Limit miesięczny"
                            value={form.monthlyLimit}
                            onChange={value => setForm({ ...form, monthlyLimit: value })}
                        />
                    </div>

                    {form.cardType === 'PREPAID' && (
                        <MoneyInput
                            label="Saldo początkowe prepaid"
                            value={form.initialBalance}
                            onChange={value => setForm({ ...form, initialBalance: value })}
                        />
                    )}

                    <div className="rounded-lg bg-slate-50 px-3 py-2 text-[11px] text-slate-500">
                        Waluta: <span className="font-semibold text-slate-700">{selectedAccount?.currency || 'EUR'}</span>
                    </div>

                    <button
                        type="submit"
                        disabled={submitting || !form.accountId}
                        className="h-10 rounded-lg border-none bg-[#1a3c8f] text-white text-[12px] font-semibold cursor-pointer disabled:opacity-50"
                    >
                        {submitting ? 'Zamawianie...' : 'Zamów kartę'}
                    </button>
                </form>

                <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
                    <div className="px-4 py-3 border-b border-slate-100 flex items-center justify-between">
                        <h2 className="text-[15px] font-semibold text-slate-900">Twoje karty</h2>
                        <span className="text-[11px] text-slate-500">{cards.length} kart</span>
                    </div>

                    {loading ? (
                        <div className="p-6 text-[12px] text-slate-500">Ładowanie kart...</div>
                    ) : cards.length === 0 ? (
                        <div className="p-6 text-[12px] text-slate-500">Nie masz jeszcze żadnych kart.</div>
                    ) : (
                        <div className="divide-y divide-slate-100">
                            {cards.map(card => (
                                <CardRow
                                    key={card.id}
                                    card={card}
                                    onActivate={() => runCardAction(activateCard, card.id)}
                                    onBlock={() => runCardAction(blockCard, card.id)}
                                    onUnblock={() => runCardAction(unblockCard, card.id)}
                                />
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

function MoneyInput({ label, value, onChange }) {
    return (
        <label className="flex flex-col gap-1.5">
            <span className="text-[11px] font-medium text-slate-500">{label}</span>
            <input
                type="number"
                min="0"
                step="0.01"
                value={value}
                onChange={event => onChange(event.target.value)}
                className="h-10 rounded-lg border border-slate-200 px-3 text-[12px]"
            />
        </label>
    )
}

function SensitiveValue({ label, value }) {
    return (
        <div className="rounded-lg bg-white/80 border border-amber-100 px-3 py-2 min-w-0">
            <p className="text-[10px] uppercase tracking-[0.08em] text-amber-700">{label}</p>
            <p className="text-[13px] font-semibold text-slate-900 truncate">{value || '-'}</p>
        </div>
    )
}

function CardRow({ card, onActivate, onBlock, onUnblock }) {
    return (
        <div className="p-4 flex items-center gap-4">
            <div className="w-[92px] h-[58px] rounded-lg bg-[#123170] text-white p-3 flex flex-col justify-between shrink-0 shadow-sm">
                <div className="w-7 h-4 rounded bg-amber-300/90" />
                <div className="text-[11px] font-semibold tracking-wide">{card.last4 ? `•••• ${card.last4}` : '••••'}</div>
            </div>

            <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                    <p className="text-[13px] font-semibold text-slate-900">{card.maskedPan || `Karta ${card.type}`}</p>
                    <span className={`rounded-full border px-2 py-0.5 text-[10px] font-semibold ${statusClass(card.status)}`}>
                        {statusLabel(card.status)}
                    </span>
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                    {card.type} · ważna do {card.expiresAt || '-'} · limit dzienny {formatEur(card.dailyLimit)}
                </p>
            </div>

            <div className="flex items-center gap-2">
                {(card.status === 'SHIPPED' || card.status === 'REQUESTED') && (
                    <button onClick={onActivate} className="h-8 px-3 rounded-lg border border-emerald-200 bg-emerald-50 text-[11px] font-semibold text-emerald-700">
                        Aktywuj
                    </button>
                )}
                {card.status === 'ACTIVE' && (
                    <button onClick={onBlock} className="h-8 px-3 rounded-lg border border-red-200 bg-red-50 text-[11px] font-semibold text-red-700">
                        Blokuj
                    </button>
                )}
                {card.status === 'BLOCKED' && (
                    <button onClick={onUnblock} className="h-8 px-3 rounded-lg border border-blue-200 bg-blue-50 text-[11px] font-semibold text-blue-700">
                        Odblokuj
                    </button>
                )}
            </div>
        </div>
    )
}
