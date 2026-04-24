export default function TopBar() {
    return (
        <div className="bg-white px-6 py-3.5 flex items-center justify-between border-b border-slate-200/70">
            <div>
                <p className="text-[15px] font-medium text-slate-800">Dzień dobry, Anno</p>
                <p className="text-[12px] text-slate-400">Czwartek, 23 kwietnia 2026</p>
            </div>
            <div className="flex items-center gap-3">

                <div className="bg-slate-100 rounded-[10px] px-3.5 py-2 flex items-center gap-2">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                        <circle cx="11" cy="11" r="8" stroke="#94a3b8" strokeWidth="1.8" />
                        <line x1="21" y1="21" x2="16.65" y2="16.65" stroke="#94a3b8" strokeWidth="1.8" />
                    </svg>
                    <span className="text-[12px] text-slate-400">Szukaj...</span>
                </div>

                <div className="relative">
                    <div className="w-9 h-9 bg-slate-100 rounded-full flex items-center justify-center cursor-pointer">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" stroke="#64748b" strokeWidth="1.8" strokeLinecap="round" />
                        </svg>
                    </div>
                    <div className="absolute top-0.5 right-0.5 w-2 h-2 bg-red-500 rounded-full border-[1.5px] border-white" />
                </div>

                <div className="w-9 h-9 bg-blue-50 rounded-full flex items-center justify-center">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="8" r="4" stroke="#2563eb" strokeWidth="1.8" />
                        <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" stroke="#2563eb" strokeWidth="1.8" strokeLinecap="round" />
                    </svg>
                </div>
            </div>
        </div>
    )
}
