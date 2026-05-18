export default function TopBar({ userEmail, onLogout }) {
    const displayName = getDisplayName(userEmail)

    return (
        <div className="bg-white px-6 py-3.5 flex items-center justify-between border-b border-slate-200/70">
            <div>
                <p className="text-[15px] font-medium text-slate-800">Dzień dobry, {displayName}</p>
                <p className="text-[12px] text-slate-400">{formatToday()}</p>
            </div>
            <div className="flex items-center gap-3">
                <button
                    onClick={onLogout}
                    className="h-9 rounded-[10px] border border-slate-200 bg-white px-3 text-[12px] font-medium text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
                >
                    Wyloguj
                </button>
            </div>
        </div>
    )
}

function getDisplayName(email = "") {
    const namePart = email.split("@")[0] || "Kliencie"
    const firstPart = namePart.split(/[._-]/)[0] || namePart
    return firstPart.charAt(0).toUpperCase() + firstPart.slice(1)
}

function formatToday() {
    return new Intl.DateTimeFormat("pl-PL", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    }).format(new Date())
}
