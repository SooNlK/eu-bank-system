const NAV_ITEMS = [
    { id: 'dashboard', label: 'Pulpit', icon: HomeIcon },
    { id: 'accounts', label: 'Rachunki', icon: CardIcon },
    { id: 'transfers', label: 'Przelewy', icon: ActivityIcon },
]

const TRANSFER_ITEMS = [
    { id: 'sepa', label: 'SEPA', icon: GlobeIcon },
    { id: 'instant', label: 'SEPA Instant', icon: BoltIcon },
    { id: 'target', label: 'TARGET2', icon: GridIcon },
]

export default function Sidebar({ activeNav, onNavChange }) {
    return (
        <aside className="flex flex-col" style={{ background: '#1a3c8f' }}>

            <div className="flex items-center gap-2.5 px-5 py-6 border-b border-white/10">
                <div className="w-[30px] h-[30px] bg-white/20 rounded-lg flex items-center justify-center shrink-0">
                    <GlobeIcon size={16} color="white" />
                </div>
                <div>
                    <span className="block text-white text-[15px] font-medium">EuroBank</span>
                    <p className="text-white/40 text-[11px]">Private Banking</p>
                </div>
            </div>

            <nav className="flex-1 px-3 py-4">
                <p className="text-white/30 text-[10px] tracking-[0.08em] uppercase px-3 pb-1.5 pt-2">
                    Menu
                </p>
                {NAV_ITEMS.map(item => (
                    <button
                        key={item.id}
                        onClick={() => onNavChange(item.id)}
                        className={`flex items-center gap-2.5 w-full px-3 py-2.5 rounded-[10px] mb-0.5 text-[13px] font-medium text-left border-none transition-colors duration-150 cursor-pointer
                            ${activeNav === item.id
                                ? 'bg-white/15 text-white'
                                : 'bg-transparent text-white/60 hover:bg-white/[0.08] hover:text-white/85'
                            }`}
                    >
                        <item.icon size={16} color="currentColor" />
                        {item.label}
                    </button>
                ))}

                <p className="text-white/30 text-[10px] tracking-[0.08em] uppercase px-3 pb-1.5 pt-2 mt-4">
                    Transfery
                </p>
                {TRANSFER_ITEMS.map(item => (
                    <button
                        key={item.id}
                        onClick={() => onNavChange(item.id)}
                        className={`flex items-center gap-2.5 w-full px-3 py-2.5 rounded-[10px] mb-0.5 text-[13px] font-medium text-left border-none transition-colors duration-150 cursor-pointer
                            ${activeNav === item.id
                                ? 'bg-white/15 text-white'
                                : 'bg-transparent text-white/60 hover:bg-white/[0.08] hover:text-white/85'
                            }`}
                    >
                        <item.icon size={16} color="currentColor" />
                        {item.label}
                    </button>
                ))}
            </nav>

            <div className="flex items-center gap-2.5 px-3 py-4 border-t border-white/10">
                <div className="w-[34px] h-[34px] bg-blue-600 rounded-full flex items-center justify-center text-[12px] font-medium text-white shrink-0">
                    AK
                </div>
                <div>
                    <p className="text-white text-[12px] font-medium">Anna Kowalska</p>
                    <p className="text-white/40 text-[11px]">Premium</p>
                </div>
                <div className="ml-auto w-2 h-2 bg-green-400 rounded-full" />
            </div>

        </aside>
    )
}

function HomeIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" stroke={color} strokeWidth="1.8" />
            <polyline points="9 22 9 12 15 12 15 22" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
        </svg>
    )
}
function CardIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <rect x="2" y="5" width="20" height="14" rx="2" stroke={color} strokeWidth="1.8" />
            <path d="M2 10h20" stroke={color} strokeWidth="1.8" />
        </svg>
    )
}
function ActivityIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2" stroke={color} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    )
}
function PiggyIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <line x1="12" y1="1" x2="12" y2="23" stroke={color} strokeWidth="1.8" />
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
        </svg>
    )
}
function TrendIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
            <polyline points="16 7 22 7 22 13" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
        </svg>
    )
}
function GlobeIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke={color} strokeWidth="1.8" />
            <path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10A15.3 15.3 0 0 1 8 12 15.3 15.3 0 0 1 12 2z" stroke={color} strokeWidth="1.8" />
        </svg>
    )
}
function BoltIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke={color} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    )
}
function GridIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <rect x="3" y="3" width="18" height="18" rx="2" stroke={color} strokeWidth="1.8" />
            <path d="M3 9h18M9 21V9" stroke={color} strokeWidth="1.8" />
        </svg>
    )
}
