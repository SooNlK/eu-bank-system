export default function QuickActions({ onNewTransfer }) {
    const actions = [
        {
            bg: 'bg-blue-50',
            iconBg: 'bg-blue-600',
            titleColor: 'text-blue-900',
            subtitleColor: 'text-blue-500',
            title: 'Nowy przelew',
            subtitle: 'SEPA / Instant / TARGET',
            icon: (
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M12 5v14M5 12l7-7 7 7" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
            ),
            onClick: onNewTransfer,
        },
        {
            bg: 'bg-green-50',
            iconBg: 'bg-green-600',
            titleColor: 'text-green-900',
            subtitleColor: 'text-green-600',
            title: 'Skanuj QR',
            subtitle: 'Płatność kodem',
            icon: (
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <rect x="3" y="3" width="18" height="18" rx="2" stroke="white" strokeWidth="2" />
                    <path d="M8 12h8M12 8v8" stroke="white" strokeWidth="2" strokeLinecap="round" />
                </svg>
            ),
        },
        {
            bg: 'bg-purple-50',
            iconBg: 'bg-purple-600',
            titleColor: 'text-purple-900',
            subtitleColor: 'text-purple-600',
            title: 'Doładuj konto',
            subtitle: 'Wpłata środków',
            icon: (
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" stroke="white" strokeWidth="2" strokeLinecap="round" />
                </svg>
            ),
        },
    ]

    return (
        <div className="bg-white rounded-2xl border border-slate-200/70 p-4 flex flex-col gap-2.5">
            <p className="text-[12px] font-medium text-slate-500 uppercase tracking-[0.07em]">
                Szybkie działania
            </p>
            {actions.map(({ bg, iconBg, titleColor, subtitleColor, title, subtitle, icon, onClick }) => (
                <button
                    key={title}
                    onClick={onClick}
                    className={`${bg} border-none rounded-xl px-3.5 py-3 cursor-pointer flex items-center gap-2.5 w-full text-left`}
                >
                    <div className={`w-[34px] h-[34px] ${iconBg} rounded-[9px] flex items-center justify-center shrink-0`}>
                        {icon}
                    </div>
                    <div>
                        <p className={`text-[13px] font-medium ${titleColor}`}>{title}</p>
                        <p className={`text-[11px] ${subtitleColor}`}>{subtitle}</p>
                    </div>
                </button>
            ))}
        </div>
    )
}
