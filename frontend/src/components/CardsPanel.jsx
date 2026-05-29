import { useEffect, useMemo, useState } from 'react'
import { activateCard, blockCard, getCards, getMyAccounts, getJuniorAccounts, issueCard, unblockCard, updateCardLimits } from '../services/account'

const CARD_TYPES = [
    { value: 'VIRTUAL', label: 'Wirtualna' },
    { value: 'PHYSICAL', label: 'Fizyczna' },
    { value: 'PREPAID', label: 'Prepaid' },
]

function formatEur(amount) {
    const value = Number(amount ?? 0)
    return `€ ${value.toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatErrorMessage(messageText) {
    if (!messageText) return "";
    const prefix = "Moduł kart odrzucił żądanie: ";
    let rawJson = messageText;
    let hasPrefix = false;
    
    if (messageText.startsWith(prefix)) {
        rawJson = messageText.substring(prefix.length);
        hasPrefix = true;
    }
    
    try {
        const parsed = JSON.parse(rawJson);
        const detail = parsed.detail || parsed.message || parsed.error;
        if (detail) {
            return hasPrefix ? `${prefix}${detail}` : detail;
        }
    } catch (e) {
        // Ignore parsing errors and return original
    }
    
    return messageText;
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

function canActivate(card) {
    return card.status === 'SHIPPED' && card.type !== 'VIRTUAL'
}

export default function CardsPanel() {
    const [accounts, setAccounts] = useState([])
    const [cards, setCards] = useState([])
    const [loading, setLoading] = useState(true)
    const [submitting, setSubmitting] = useState(false)
    const [message, setMessage] = useState(null)
    const [sensitiveCard, setSensitiveCard] = useState(null)
    const [selectedCardId, setSelectedCardId] = useState(null)
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
            const [myAccs, cardList] = await Promise.all([getMyAccounts(), getCards()])
            
            const isChild = myAccs.length > 0 && myAccs.every(a => a.type === 'JUNIOR')
            let allAccs = [...myAccs]
            
            if (!isChild) {
                try {
                    const juniorAccs = await getJuniorAccounts()
                    allAccs = [...allAccs, ...juniorAccs]
                } catch (juniorError) {
                    console.error("Failed to fetch junior accounts:", juniorError)
                }
            }
            
            setAccounts(allAccs)
            setCards(cardList)
            setForm(current => ({
                ...current,
                accountId: current.accountId || allAccs[0]?.id || '',
            }))
            setSelectedCardId(current => current || cardList[0]?.id || null)
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

    const selectedCard = useMemo(
        () => cards.find(c => c.id === selectedCardId),
        [cards, selectedCardId]
    )

    const isJuniorUser = useMemo(() => {
        return accounts.length > 0 && accounts.every(a => a.type === 'JUNIOR')
    }, [accounts])

    const availableCardTypes = useMemo(() => {
        if (!selectedAccount) return CARD_TYPES
        if (selectedAccount.type === 'STANDARD') {
            return CARD_TYPES.filter(type => type.value !== 'PREPAID')
        }
        return CARD_TYPES.filter(type => type.value === 'PREPAID')
    }, [selectedAccount])

    useEffect(() => {
        if (availableCardTypes.length > 0) {
            const isValid = availableCardTypes.some(t => t.value === form.cardType)
            if (!isValid) {
                setForm(f => ({ ...f, cardType: availableCardTypes[0].value }))
            }
        }
    }, [availableCardTypes, form.cardType])

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
            if (result && result.card) {
                setSelectedCardId(result.card.id)
            }
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

    async function handleUpdateLimits(cardId, dailyLimit, monthlyLimit) {
        setMessage(null)
        try {
            await updateCardLimits(cardId, dailyLimit, monthlyLimit)
            await loadData()
            setMessage({ type: 'success', text: 'Limity karty zostały pomyślnie zaktualizowane.' })
        } catch (error) {
            setMessage({ type: 'error', text: error.message })
            throw error
        }
    }

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-[22px] font-semibold text-slate-900">Karty płatnicze</h1>
                    <p className="text-[12px] text-slate-500 mt-1">Wydawanie i obsługa kart przez zewnętrzną sieć kartową.</p>
                </div>
            </div>

            {message && (
                <div className={`rounded-xl border p-4 flex items-start gap-3 shadow-sm transition-all duration-300 ${
                    message.type === 'error' 
                        ? 'bg-rose-50/90 border-rose-200/70 text-rose-900' 
                        : 'bg-emerald-50/90 border-emerald-200/70 text-emerald-900'
                }`}>
                    {message.type === 'error' ? (
                        <svg className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                    ) : (
                        <svg className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                    )}
                    
                    <div className="flex-1 text-[13px] font-medium leading-relaxed">
                        {formatErrorMessage(message.text)}
                    </div>
                    <button 
                        onClick={() => setMessage(null)}
                        className="text-slate-400 hover:text-slate-600 border-none bg-transparent cursor-pointer leading-none text-[16px] px-1 focus:outline-none"
                    >
                        ×
                    </button>
                </div>
            )}
            <div className="flex flex-col gap-4">
                <div className={`grid ${isJuniorUser ? 'grid-cols-1' : 'grid-cols-[360px_1fr]'} gap-4 items-stretch`}>
                    {!isJuniorUser && (
                        <form onSubmit={handleIssue} className="bg-white rounded-lg border border-slate-200 p-4 flex flex-col justify-between h-full">
                            <div className="flex flex-col gap-3">
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
                                    <div className="flex gap-1 mt-1.5 rounded-lg bg-slate-100 p-1">
                                        {availableCardTypes.map(type => (
                                            <button
                                                key={type.value}
                                                type="button"
                                                onClick={() => setForm({ ...form, cardType: type.value })}
                                                className={`h-8 flex-1 rounded-md border-none text-[11px] font-medium cursor-pointer transition-all ${form.cardType === type.value ? 'bg-white text-blue-750 shadow-sm font-semibold' : 'bg-transparent text-slate-500'}`}
                                            >
                                                {type.label}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {form.cardType === 'PREPAID' && (
                                    <MoneyInput
                                        label="Saldo początkowe prepaid"
                                        value={form.initialBalance}
                                        onChange={value => setForm({ ...form, initialBalance: value })}
                                    />
                                )}
                            </div>

                            <div className="flex flex-col gap-3 mt-4 pt-2">
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
                            </div>
                        </form>
                    )}

                    <div className="flex flex-col gap-4 h-full">
                        <style dangerouslySetInnerHTML={{ __html: `
                            .card-3d-container {
                                perspective: 1000px;
                                width: 320px;
                                height: 200px;
                                cursor: pointer;
                            }
                            .card-3d-flipper {
                                position: relative;
                                width: 100%;
                                height: 100%;
                                transition: transform 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
                                transform-style: preserve-3d;
                            }
                            .card-3d-flipper.flipped {
                                transform: rotateY(180deg);
                            }
                            .card-3d-face {
                                position: absolute;
                                width: 100%;
                                height: 100%;
                                backface-visibility: hidden;
                                border-radius: 16px;
                                box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
                                padding: 20px;
                                display: flex;
                                flex-direction: column;
                                justify-content: space-between;
                                color: white;
                                overflow: hidden;
                            }
                            .card-3d-front {
                                background: linear-gradient(135deg, #1e3a8a 0%, #0f172a 100%);
                                border: 1px solid rgba(255, 255, 255, 0.1);
                            }
                            .card-3d-front.card-type-prepaid {
                                background: linear-gradient(135deg, #064e3b 0%, #022c22 100%);
                            }
                            .card-3d-front.card-type-virtual {
                                background: linear-gradient(135deg, #4c1d95 0%, #1e1b4b 100%);
                            }
                            .card-3d-back {
                                background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
                                transform: rotateY(180deg);
                                border: 1px solid rgba(255, 255, 255, 0.1);
                                padding: 0;
                            }
                            .card-3d-shine {
                                position: absolute;
                                top: 0;
                                left: 0;
                                right: 0;
                                bottom: 0;
                                background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 50%, rgba(255,255,255,0) 100%);
                                pointer-events: none;
                                border-radius: 16px;
                            }
                            .card-chip {
                                width: 44px;
                                height: 32px;
                                border-radius: 6px;
                                background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
                                position: relative;
                                border: 1px solid rgba(0,0,0,0.15);
                            }
                            .card-chip::after {
                                content: '';
                                position: absolute;
                                top: 4px;
                                left: 8px;
                                right: 8px;
                                bottom: 4px;
                                border: 1px solid rgba(255, 255, 255, 0.35);
                                border-radius: 4px;
                            }
                            .card-magstripe {
                                width: 100%;
                                height: 36px;
                                background: #090d16;
                                margin-top: 24px;
                            }
                            .card-sigstrip {
                                width: calc(100% - 40px);
                                height: 32px;
                                background: rgba(255, 255, 255, 0.85);
                                margin-top: 16px;
                                margin-left: 20px;
                                margin-right: 20px;
                                display: flex;
                                align-items: center;
                                justify-content: flex-end;
                                padding-right: 8px;
                                border-radius: 4px;
                            }
                            .card-cvv-box {
                                background: #fff;
                                color: #0f172a;
                                font-family: monospace;
                                padding: 2px 8px;
                                border: 1px solid #94a3b8;
                                font-weight: bold;
                                border-radius: 3px;
                                font-size: 13px;
                                letter-spacing: 1px;
                            }
                        ` }} />

                        {selectedCard && (
                            <CardDetailsPanel
                                card={selectedCard}
                                sensitiveCard={sensitiveCard}
                                onActivate={() => runCardAction(activateCard, selectedCard.id)}
                                onBlock={() => runCardAction(blockCard, selectedCard.id)}
                                onUnblock={() => runCardAction(unblockCard, selectedCard.id)}
                                accounts={accounts}
                                onUpdateLimits={handleUpdateLimits}
                                isJuniorUser={isJuniorUser}
                            />
                        )}
                    </div>
                </div>

                {/* Twoje karty bottom row */}
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
                                    isSelected={selectedCardId === card.id}
                                    onSelect={() => setSelectedCardId(card.id)}
                                    onActivate={() => runCardAction(activateCard, card.id)}
                                    onBlock={() => runCardAction(blockCard, card.id)}
                                    onUnblock={() => runCardAction(unblockCard, card.id)}
                                    balance={card.balance}
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


function getCardCredentials(card, sensitiveCard) {
    if (sensitiveCard && sensitiveCard.card?.id === card.id) {
        return {
            fullPan: sensitiveCard.fullPan,
            cvv: sensitiveCard.cvv,
            isDemo: false
        }
    }
    const idHash = card.id ? card.id.replace(/[^0-9a-fA-F]/g, '') : 'default'
    const mockMiddle = idHash.substring(0, 8).replace(/[^0-9]/g, '3').padEnd(8, '4')
    const maskedPan = card.maskedPan || ''
    
    let fullPan = ''
    let middleIdx = 0
    for (let i = 0; i < maskedPan.length; i++) {
        if (maskedPan[i] === '*') {
            fullPan += mockMiddle[middleIdx % mockMiddle.length]
            middleIdx++
        } else {
            fullPan += maskedPan[i]
        }
    }
    if (!fullPan || fullPan.includes('*')) {
        fullPan = `4300 0112 3456 ${card.last4 || '7375'}`
    }
    
    let parsedHash = 123
    if (idHash !== 'default') {
        const slice = idHash.substring(8, 12)
        parsedHash = parseInt(slice, 16)
        if (isNaN(parsedHash)) parsedHash = 123
    }
    const mockCvv = String(parsedHash % 900 + 100)
    
    return {
        fullPan,
        cvv: mockCvv,
        isDemo: true
    }
}

function CardDetailsPanel({ card, sensitiveCard, onActivate, onBlock, onUnblock, accounts, onUpdateLimits, isJuniorUser }) {
    const [flipped, setFlipped] = useState(false)
    const [isEditingLimits, setIsEditingLimits] = useState(false)
    const [editDaily, setEditDaily] = useState(card.dailyLimit || '')
    const [editMonthly, setEditMonthly] = useState(card.monthlyLimit || '')
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        setEditDaily(card.dailyLimit || '')
        setEditMonthly(card.monthlyLimit || '')
        setIsEditingLimits(false)
    }, [card])

    async function handleSaveLimits() {
        setSaving(true)
        try {
            await onUpdateLimits(card.id, editDaily, editMonthly)
            setIsEditingLimits(false)
        } catch (error) {
            // Keep form open
        } finally {
            setSaving(false)
        }
    }

    const cardAccount = accounts.find(acc => acc.id === card.accountId)
    const ownerName = cardAccount?.ownerName || "HANS MUELLER"
    
    const credentials = useMemo(() => getCardCredentials(card, sensitiveCard), [card, sensitiveCard])
    
    const formattedPan = useMemo(() => {
        const pan = credentials.fullPan.replace(/\s/g, '')
        const blocks = []
        for (let i = 0; i < pan.length; i += 4) {
            blocks.push(pan.substring(i, i + 4))
        }
        return blocks.join('  ')
    }, [credentials.fullPan])
    
    const formattedExpiry = useMemo(() => {
        if (!card.expiresAt) return '05/31'
        const parts = card.expiresAt.split('-')
        if (parts.length >= 2) {
            return `${parts[1]}/${parts[0].substring(2)}`
        }
        return '05/31'
    }, [card.expiresAt])

    const cardTypeLabel = {
        VIRTUAL: 'Wirtualna',
        PHYSICAL: 'Fizyczna',
        PREPAID: 'Prepaid'
    }[card.type] || card.type

    return (
        <div className="bg-white rounded-lg border border-slate-200 p-6 flex flex-col md:flex-row gap-6 items-center justify-between shadow-sm h-full">
            <div className="flex flex-col items-center gap-2 select-none">
                <div 
                    className="card-3d-container"
                    onClick={() => setFlipped(!flipped)}
                >
                    <div className={`card-3d-flipper ${flipped ? 'flipped' : ''}`}>
                        {/* FRONT FACE */}
                        <div className={`card-3d-face card-3d-front ${
                            card.type === 'PREPAID' ? 'card-type-prepaid' : 
                            card.type === 'VIRTUAL' ? 'card-type-virtual' : ''
                        }`}>
                            <div className="card-3d-shine" />
                            
                            {/* Top row */}
                            <div className="flex justify-between items-start w-full">
                                <div>
                                    <p className="text-[12px] font-bold tracking-wider text-white">EuroBank</p>
                                    <p className="text-[8px] text-white/60 tracking-wider">Private Banking</p>
                                </div>
                                <span className="text-[9px] font-semibold bg-white/10 px-2 py-0.5 rounded backdrop-blur-sm border border-white/10">
                                    {cardTypeLabel}
                                </span>
                            </div>
                            
                            {/* Chip & Contactless */}
                            <div className="flex justify-between items-center w-full mt-2">
                                <div className="card-chip" />
                                <svg className="w-6 h-6 text-white/60" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M5 12a10 10 0 0 1 10-10m-10 14a6 6 0 0 1 6-6m-6 10a2 2 0 0 1 2-2" strokeLinecap="round" strokeLinejoin="round" />
                                </svg>
                            </div>
                            
                            {/* Card Number */}
                            <div className="w-full text-center mt-3">
                                <span className="font-mono tracking-widest text-[16px] text-white font-semibold drop-shadow-sm select-all">
                                    ••••  ••••  ••••  {card.last4 || '7375'}
                                </span>
                            </div>
                            
                            {/* Expiry & Holder */}
                            <div className="flex justify-between items-end w-full mt-2">
                                <div>
                                    <p className="text-[7px] text-white/40 uppercase tracking-widest font-sans">Właściciel</p>
                                    <p className="text-[12px] tracking-wide text-white/95 font-medium truncate max-w-[240px] uppercase font-sans">
                                        {ownerName}
                                    </p>
                                </div>
                            </div>
                        </div>
                        
                        {/* BACK FACE */}
                        <div className="card-3d-face card-3d-back">
                            <div className="card-3d-shine" />
                            <div className="card-magstripe" />
                            
                            <div className="card-sigstrip">
                                <span className="text-[9px] text-slate-400 select-none mr-2 font-sans">CVV</span>
                                <div className="card-cvv-box select-all">
                                    {credentials.cvv}
                                </div>
                            </div>
                            
                            {/* Dynamic Credentials (Full PAN & Expiry) */}
                            <div className="px-5 mt-2 flex justify-between items-center w-full text-left">
                                <div>
                                    <p className="text-[6px] text-white/45 uppercase tracking-widest font-sans">Numer karty</p>
                                    <p className="font-mono text-[11px] text-white font-semibold tracking-wider select-all">{formattedPan}</p>
                                </div>
                                <div className="text-right">
                                    <p className="text-[6px] text-white/45 uppercase tracking-widest font-sans">Ważna do</p>
                                    <p className="font-mono text-[11px] text-white font-semibold tracking-wider select-all">{formattedExpiry}</p>
                                </div>
                            </div>
                            
                            <div className="px-5 mt-2.5 flex justify-between items-end w-full">
                                <div className="text-[8px] text-white/40 leading-normal max-w-[200px] text-left">
                                    EuroBank SA. Użycie podlega regulaminowi banku.
                                </div>
                                <div className="flex gap-1 mb-1">
                                    <div className="w-5 h-5 rounded-full bg-red-500/80 -mr-1.5" />
                                    <div className="w-5 h-5 rounded-full bg-amber-500/80" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <p className="text-[10px] text-slate-400 mt-1 flex items-center gap-1">
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.213 6H16" />
                    </svg>
                    Kliknij kartę, aby odwrócić i zobaczyć pełne dane (PAN, CVV, datę)
                </p>
            </div>
            
            {/* Details & Actions */}
            <div className="flex-1 flex flex-col gap-4 min-w-0 md:pl-6 border-t md:border-t-0 md:border-l border-slate-100 pt-4 md:pt-0 w-full text-left">
                <div>
                    <div className="flex items-center gap-2">
                        <h3 className="text-[16px] font-semibold text-slate-900">
                            {ownerName}
                        </h3>
                        <span className={`rounded-full border px-2 py-0.5 text-[10px] font-semibold ${statusClass(card.status)}`}>
                            {statusLabel(card.status)}
                        </span>
                    </div>
                    <p className="text-[12px] text-slate-500 mt-1">
                        Rachunek: {cardAccount?.accountNumber || '-'}
                    </p>
                </div>
                
                {card.type === 'PREPAID' ? (
                    <div className="flex flex-col gap-2">
                        <div className="bg-emerald-50/70 border border-emerald-100/80 rounded-lg p-3">
                            <span className="text-[10px] text-emerald-700 uppercase tracking-wider font-semibold block">Środki na karcie</span>
                            <span className="text-[20px] font-bold text-emerald-950 block mt-1">{formatEur(card.balance)}</span>
                        </div>
                    </div>
                ) : !isEditingLimits ? (
                    <div className="flex flex-col gap-2">
                        <div className="grid grid-cols-2 gap-4 bg-slate-50 rounded-lg p-3">
                            <div>
                                <span className="text-[10px] text-slate-400 uppercase tracking-wider block">Limit dzienny</span>
                                <span className="text-[13px] font-semibold text-slate-800">{formatEur(card.dailyLimit)}</span>
                            </div>
                            <div>
                                <span className="text-[10px] text-slate-400 uppercase tracking-wider block">Limit miesięczny</span>
                                <span className="text-[13px] font-semibold text-slate-800">{formatEur(card.monthlyLimit)}</span>
                            </div>
                        </div>
                        {!isJuniorUser && (
                            <button
                                type="button"
                                onClick={() => setIsEditingLimits(true)}
                                className="h-8 px-3 rounded-lg border border-slate-200 bg-white text-[11px] font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer self-start flex items-center gap-1 transition-colors mt-1"
                            >
                                <svg className="w-3.5 h-3.5 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                </svg>
                                Zmień limity
                            </button>
                        )}
                    </div>
                ) : (
                    <div className="flex flex-col gap-3 bg-slate-50 rounded-xl p-3 border border-slate-200">
                        <span className="text-[11px] font-bold text-slate-800 block">Edycja limitów transakcyjnych</span>
                        <div className="grid grid-cols-2 gap-3">
                            <label className="flex flex-col gap-1">
                                <span className="text-[9px] font-medium text-slate-500 uppercase tracking-wider">Limit dzienny</span>
                                <input 
                                    type="number" 
                                    min="0"
                                    step="1"
                                    value={editDaily}
                                    onChange={e => setEditDaily(e.target.value)}
                                    className="h-8 rounded-lg border border-slate-200 px-2 text-[12px] bg-white font-semibold text-slate-800"
                                />
                            </label>
                            <label className="flex flex-col gap-1">
                                <span className="text-[9px] font-medium text-slate-500 uppercase tracking-wider">Limit miesięczny</span>
                                <input 
                                    type="number" 
                                    min="0"
                                    step="1"
                                    value={editMonthly}
                                    onChange={e => setEditMonthly(e.target.value)}
                                    className="h-8 rounded-lg border border-slate-200 px-2 text-[12px] bg-white font-semibold text-slate-800"
                                />
                            </label>
                        </div>
                        <div className="flex gap-2 justify-end mt-1">
                            <button 
                                type="button"
                                onClick={() => setIsEditingLimits(false)}
                                className="h-7 px-3 rounded-lg border border-slate-200 bg-white text-[11px] font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer"
                            >
                                Anuluj
                            </button>
                            <button 
                                type="button"
                                disabled={saving}
                                onClick={handleSaveLimits}
                                className="h-7 px-3 rounded-lg border-none bg-[#1a3c8f] hover:bg-[#123170] text-[11px] font-semibold text-white cursor-pointer disabled:opacity-50"
                            >
                                {saving ? 'Zapisywanie...' : 'Zapisz'}
                            </button>
                        </div>
                    </div>
                )}
                
                <div className="flex items-center gap-2 mt-2">
                    {canActivate(card) && (
                        <button 
                            onClick={onActivate} 
                            className="flex-1 h-10 rounded-lg border border-emerald-200 bg-emerald-50 text-[12px] font-semibold text-emerald-700 hover:bg-emerald-100 transition-colors"
                        >
                            Aktywuj kartę
                        </button>
                    )}
                    {card.type === 'VIRTUAL' && card.status !== 'ACTIVE' && card.status !== 'BLOCKED' && (
                        <div className="flex-1 rounded-lg border border-amber-100 bg-amber-50 px-3 py-2 text-[11px] font-medium text-amber-700 text-center">
                            Aktywacja automatyczna
                        </div>
                    )}
                    {card.type !== 'VIRTUAL' && (card.status === 'REQUESTED' || card.status === 'PRODUCING') && (
                        <div className="flex-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-[11px] font-medium text-slate-500 text-center">
                            Oczekuje na wysyłkę
                        </div>
                    )}
                    {card.status === 'ACTIVE' && (
                        <button 
                            onClick={onBlock} 
                            className="flex-1 h-10 rounded-lg border border-red-200 bg-red-50 text-[12px] font-semibold text-red-700 hover:bg-red-100 transition-colors"
                        >
                            Blokuj kartę
                        </button>
                    )}
                    {card.status === 'BLOCKED' && (
                        <button 
                            onClick={onUnblock} 
                            className="flex-1 h-10 rounded-lg border border-blue-200 bg-blue-50 text-[12px] font-semibold text-blue-700 hover:bg-blue-100 transition-colors"
                        >
                            Odblokuj kartę
                        </button>
                    )}
                </div>
            </div>
        </div>
    )
}

function CardRow({ card, isSelected, onSelect, onActivate, onBlock, onUnblock, balance }) {
    return (
        <div 
            onClick={onSelect}
            className={`p-4 flex items-center gap-4 cursor-pointer hover:bg-slate-50 transition-colors ${isSelected ? 'bg-blue-50/50 border-l-4 border-[#1a3c8f]' : ''}`}
        >
            <div className="w-[92px] h-[58px] rounded-lg bg-[#123170] text-white p-3 flex flex-col justify-between shrink-0 shadow-sm">
                <div className="w-7 h-4 rounded bg-amber-300/90" />
                <div className="text-[11px] font-semibold tracking-wide">{card.last4 ? `•••• ${card.last4}` : '••••'}</div>
            </div>

            <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                    <p className="text-[13px] font-semibold text-slate-900">
                        {card.last4 ? `•••• •••• •••• ${card.last4}` : (card.maskedPan || `Karta ${card.type}`)}
                    </p>
                    <span className={`rounded-full border px-2 py-0.5 text-[10px] font-semibold ${statusClass(card.status)}`}>
                        {statusLabel(card.status)}
                    </span>
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                    {card.type === 'PREPAID' ? (
                        <>Prepaid · ważna do {card.expiresAt || '-'} · saldo {formatEur(balance)}</>
                    ) : (
                        <>{card.type === 'VIRTUAL' ? 'Wirtualna' : 'Fizyczna'} · ważna do {card.expiresAt || '-'} · limit dzienny {formatEur(card.dailyLimit)}</>
                    )}
                </p>
            </div>

            <div className="flex items-center gap-2">
                {canActivate(card) && (
                    <button 
                        onClick={(e) => { e.stopPropagation(); onActivate(); }} 
                        className="h-8 px-3 rounded-lg border border-emerald-200 bg-emerald-50 text-[11px] font-semibold text-emerald-700 hover:bg-emerald-100 transition-colors"
                    >
                        Aktywuj
                    </button>
                )}
                {card.type === 'VIRTUAL' && card.status !== 'ACTIVE' && card.status !== 'BLOCKED' && (
                    <span className="h-8 px-3 rounded-lg border border-amber-100 bg-amber-50 text-[11px] font-semibold text-amber-700 flex items-center">
                        Auto
                    </span>
                )}
                {card.status === 'ACTIVE' && (
                    <button 
                        onClick={(e) => { e.stopPropagation(); onBlock(); }} 
                        className="h-8 px-3 rounded-lg border border-red-200 bg-red-50 text-[11px] font-semibold text-red-700 hover:bg-red-100 transition-colors"
                    >
                        Blokuj
                    </button>
                )}
                {card.status === 'BLOCKED' && (
                    <button 
                        onClick={(e) => { e.stopPropagation(); onUnblock(); }} 
                        className="h-8 px-3 rounded-lg border border-blue-200 bg-blue-50 text-[11px] font-semibold text-blue-700 hover:bg-blue-100 transition-colors"
                    >
                        Odblokuj
                    </button>
                )}
            </div>
        </div>
    )
}
