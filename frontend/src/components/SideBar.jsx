const TRANSFER_IDS = ['sepa', 'instant', 'target', 'internal', 'transfers']

export default function Sidebar({ activeNav, onNavChange, userEmail, isJunior, pendingCount = 0 }) {
    const isTransfersActive = TRANSFER_IDS.includes(activeNav)

    const displayName = getDisplayName(userEmail)
    const initials = getInitials(userEmail)

    const items = isJunior ? [
        { id: 'dashboard', label: 'Pulpit 🧸', icon: HomeIcon },
        { id: 'cards', label: 'Karta 💳', icon: CardIcon },
        { id: 'internal', label: 'Przelew 🚀', icon: RocketIcon }
    ] : [
        { id: 'dashboard', label: 'Pulpit', icon: HomeIcon },
        { id: 'cards', label: 'Karty', icon: CardIcon },
        { id: 'transfers', label: 'Przelewy', icon: ActivityIcon },
        { id: 'junior', label: 'Strefa Rodzica', icon: ChildIcon }
    ]

    return (
        <aside 
            className="flex flex-col" 
            style={{ background: isJunior ? 'linear-gradient(135deg, #7c3aed 0%, #0d9488 100%)' : '#1a3c8f' }}
        >

            <div className="flex items-center gap-2.5 px-5 py-6 border-b border-white/10">
                <div className="w-[30px] h-[30px] bg-white/20 rounded-lg flex items-center justify-center shrink-0">
                    <GlobeIcon size={16} color="white" />
                </div>
                <div>
                    <span className="block text-white text-[15px] font-medium">
                        {isJunior ? 'EuroBank Junior' : 'EuroBank'}
                    </span>
                    <p className="text-white/40 text-[11px]">
                        {isJunior ? 'Młody Oszczędzający' : 'Private Banking'}
                    </p>
                </div>
            </div>

            <nav className="flex-1 px-3 py-4">
                <p className="text-white/30 text-[10px] tracking-[0.08em] uppercase px-3 pb-1.5 pt-2">
                    Menu
                </p>
                {items.map(item => {
                    const isActive = item.id === 'transfers'
                        ? isTransfersActive
                        : activeNav === item.id
                    return (
                        <button
                            key={item.id}
                            onClick={() => onNavChange(item.id)}
                            className={`flex items-center gap-2.5 w-full px-3 py-2.5 rounded-[10px] mb-0.5 text-[13px] font-medium text-left border-none transition-colors duration-150 cursor-pointer
                                ${isActive
                                    ? 'bg-white/15 text-white'
                                    : 'bg-transparent text-white/60 hover:bg-white/[0.08] hover:text-white/85'
                                }`}
                        >
                            <item.icon size={16} color="currentColor" />
                            <span className="flex-1">{item.label}</span>
                            {item.id === 'junior' && pendingCount > 0 && (
                                <span className="bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full animate-pulse shrink-0">
                                    {pendingCount}
                                </span>
                            )}
                        </button>
                    )
                })}
            </nav>

            <div className="flex items-center gap-2.5 px-3 py-4 border-t border-white/10">
                <div className={`w-[34px] h-[34px] ${isJunior ? 'bg-teal-500' : 'bg-blue-600'} rounded-full flex items-center justify-center text-[12px] font-medium text-white shrink-0`}>
                    {initials}
                </div>
                <div>
                    <p className="text-white text-[12px] font-medium">{displayName}</p>
                    <p className="text-white/40 text-[11px] truncate max-w-[130px]">{userEmail}</p>
                </div>
                <div className="ml-auto w-2 h-2 bg-green-400 rounded-full" />
            </div>

        </aside>
    )
}

function getDisplayName(email = "") {
    const namePart = email.split("@")[0] || "Klient"
    return namePart
        .split(/[._-]/)
        .filter(Boolean)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ") || "Klient"
}

function getInitials(email = "") {
    const parts = (email.split("@")[0] || "KB").split(/[._-]/).filter(Boolean)
    return parts
        .slice(0, 2)
        .map(part => part.charAt(0).toUpperCase())
        .join("")
}

function HomeIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" stroke={color} strokeWidth="1.8" />
            <polyline points="9 22 9 12 15 12 15 22" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
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

function CardIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <rect x="3" y="5" width="18" height="14" rx="2.5" stroke={color} strokeWidth="1.8" />
            <path d="M3 10h18M7 15h4" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
        </svg>
    )
}

function ChildIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="7" r="4" stroke={color} strokeWidth="1.8" />
            <path d="M5 22v-2a7 7 0 0 1 14 0v2" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
            <path d="M12 11v4" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
        </svg>
    )
}

function RocketIcon({ size = 16, color = 'currentColor' }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
            <path d="M4.5 16.5c-1.5 1.5-2.5 3.5-2.5 5.5 2 0 4-1 5.5-2.5" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
            <path d="M12 15l-3-3M18 3c-4.5 0-9 4.5-9 9 0 .5.5 1 1 1s.5-.5 1-1c1.5-1.5 3-1.5 4.5-3s1.5-3 3-4.5" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
            <circle cx="15.5" cy="8.5" r="1.5" fill={color} />
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
