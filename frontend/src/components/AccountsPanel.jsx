const ACCOUNTS = [
    {
        id: 'main',
        label: 'Rachunek główny',
        type: 'Rachunek bieżący',
        iban: 'PL61 1090 1014 0000 0712 1981 2874',
        bic: 'WBKPPLPP',
        balance: 18420.0,
        currency: 'EUR',
        color: '#1a3c8f',
        change: '+€ 500,00',
        positive: true,
    },
    {
        id: 'savings',
        label: 'Konto oszczędnościowe',
        type: 'Oszczędności',
        iban: 'PL83 1090 2590 0000 0001 4200 4000',
        bic: 'WBKPPLPP',
        balance: 6430.37,
        currency: 'EUR',
        color: '#0f766e',
        change: '+€ 12,50 odsetek',
        positive: true,
    },
]

function formatCurrency(value, currency = 'EUR') {
    return new Intl.NumberFormat('pl-PL', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
    }).format(value)
}

function CopyButton({ text }) {
    const handleCopy = () => {
        navigator.clipboard.writeText(text).catch(() => {})
    }
    return (
        <button
            type="button"
            onClick={handleCopy}
            title="Kopiuj"
            className="ml-1 text-slate-300 hover:text-slate-500 transition-colors bg-transparent border-none cursor-pointer p-0"
        >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                <rect x="9" y="9" width="13" height="13" rx="2" stroke="currentColor" strokeWidth="1.8" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" stroke="currentColor" strokeWidth="1.8" />
            </svg>
        </button>
    )
}

export default function AccountsPanel() {
    const totalBalance = ACCOUNTS.reduce((sum, a) => sum + a.balance, 0)

    return (
        <div className="flex flex-col gap-4">
            {/* Nagłówek łączny */}
            <div
                className="rounded-2xl p-6 relative overflow-hidden"
                style={{ background: '#1a3c8f' }}
            >
                <div className="absolute -top-5 -right-5 w-[120px] h-[120px] rounded-full bg-white/[0.05]" />
                <div className="absolute -bottom-8 right-24 w-[80px] h-[80px] rounded-full bg-white/[0.04]" />
                <p className="text-white/55 text-[11px] tracking-[0.08em] uppercase mb-1">Łączne saldo</p>
                <p className="text-white text-[32px] font-medium mb-1 tracking-[-0.5px]">
                    {formatCurrency(totalBalance)}
                </p>
                <p className="text-white/40 text-[12px]">{ACCOUNTS.length} rachunki · EuroBank Private Banking</p>
            </div>

            {/* Lista rachunków */}
            <div className="flex flex-col gap-3">
                {ACCOUNTS.map((acc) => (
                    <div key={acc.id} className="bg-white rounded-2xl border border-slate-200/70 p-5">
                        <div className="flex items-start gap-4">
                            {/* Ikona */}
                            <div
                                className="w-11 h-11 rounded-[12px] flex items-center justify-center shrink-0"
                                style={{ background: acc.color + '18' }}
                            >
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <rect x="2" y="7" width="20" height="14" rx="2" stroke={acc.color} strokeWidth="1.8" />
                                    <path d="M16 14a1 1 0 1 1 0-2 1 1 0 0 1 0 2z" fill={acc.color} />
                                    <path d="M6 7V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2" stroke={acc.color} strokeWidth="1.8" />
                                </svg>
                            </div>

                            {/* Treść */}
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center justify-between gap-2">
                                    <div>
                                        <p className="text-[14px] font-semibold text-slate-800">{acc.label}</p>
                                        <span className="text-[10px] text-slate-400 bg-slate-100 rounded-[5px] px-1.5 py-0.5 font-medium">
                                            {acc.type}
                                        </span>
                                    </div>
                                    <div className="text-right shrink-0">
                                        <p className="text-[20px] font-semibold text-slate-900 tracking-[-0.5px]">
                                            {formatCurrency(acc.balance, acc.currency)}
                                        </p>
                                        <p className={`text-[11px] font-medium ${acc.positive ? 'text-green-600' : 'text-red-500'}`}>
                                            {acc.change} (ten miesiąc)
                                        </p>
                                    </div>
                                </div>

                                {/* Detale */}
                                <div className="mt-3 grid grid-cols-2 gap-x-6 gap-y-2 border-t border-slate-100 pt-3">
                                    <div>
                                        <p className="text-[10px] text-slate-400 mb-0.5 uppercase tracking-wide">IBAN</p>
                                        <p className="text-[11px] font-mono text-slate-700 flex items-center">
                                            {acc.iban}
                                            <CopyButton text={acc.iban.replace(/\s/g, '')} />
                                        </p>
                                    </div>
                                    <div>
                                        <p className="text-[10px] text-slate-400 mb-0.5 uppercase tracking-wide">BIC / SWIFT</p>
                                        <p className="text-[11px] font-mono text-slate-700 flex items-center">
                                            {acc.bic}
                                            <CopyButton text={acc.bic} />
                                        </p>
                                    </div>
                                    <div>
                                        <p className="text-[10px] text-slate-400 mb-0.5 uppercase tracking-wide">Waluta</p>
                                        <p className="text-[11px] text-slate-700">{acc.currency}</p>
                                    </div>
                                    <div>
                                        <p className="text-[10px] text-slate-400 mb-0.5 uppercase tracking-wide">Bank</p>
                                        <p className="text-[11px] text-slate-700">EuroBank S.A.</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}
