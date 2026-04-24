export default function BalanceCard() {
    return (
        <div
            className="rounded-[18px] p-6 relative overflow-hidden"
            style={{ background: '#1a3c8f' }}
        >
            <div className="absolute -top-5 -right-5 w-[100px] h-[100px] rounded-full bg-white/[0.06]" />
            <div className="absolute -bottom-8 right-20 w-[70px] h-[70px] rounded-full bg-white/[0.04]" />

            <p className="text-white/55 text-[11px] tracking-[0.08em] uppercase mb-1">
                Saldo ogółem
            </p>
            <p className="text-white text-[32px] font-medium mb-1.5 tracking-[-0.5px]">
                € 24 850,<span className="text-[22px] opacity-75">37</span>
            </p>

            <div className="flex items-center gap-2 mb-5">
                <div className="bg-green-400/20 rounded-[5px] px-1.5 py-0.5 flex items-center gap-1">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                        <polyline points="18 15 12 9 6 15" stroke="#4ade80" strokeWidth="2.5" strokeLinecap="round" />
                    </svg>
                    <span className="text-green-400 text-[11px] font-medium">+2.4%</span>
                </div>
                <span className="text-white/40 text-[11px]">vs. ubiegły miesiąc</span>
            </div>

            <div className="grid grid-cols-3 gap-2">
                {[
                    { label: 'Rachunek główny', value: '€ 18 420,00' },
                ].map(({ label, value }) => (
                    <div key={label} className="bg-white/10 rounded-[10px] p-2.5">
                        <p className="text-white/50 text-[10px] mb-0.5">{label}</p>
                        <p className="text-white text-[14px] font-medium">{value}</p>
                    </div>
                ))}
            </div>
        </div>
    )
}
