export default function BalanceCard({ accounts = [], loading }) {
    // Calculate total balance across all accounts
    let totalBalance = 0;
    let mainCurrency = 'EUR'; // Default to EUR or take from first account
    if (accounts && accounts.length > 0) {
        totalBalance = accounts.reduce((sum, acc) => sum + (acc.balance || 0), 0);
        mainCurrency = accounts[0].currency || 'EUR';
    }

    const formatCurrency = (amount, currency) => {
        return new Intl.NumberFormat('pl-PL', { style: 'currency', currency: currency }).format(amount);
    }

    // Format total balance for large display
    const formattedTotal = new Intl.NumberFormat('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(totalBalance);
    const [intPart, fracPart] = formattedTotal.split(',');

    // Map accounts for smaller cards
    const accountCards = accounts.map(acc => ({
        label: acc.type === 'STANDARD' ? 'Rachunek główny' : acc.type,
        value: formatCurrency(acc.balance, acc.currency),
        id: acc.id
    }));

    if (loading) {
        return (
            <div className="rounded-[18px] p-6 relative overflow-hidden bg-slate-200 animate-pulse h-full"></div>
        );
    }

    return (
        <div
            className="rounded-[18px] p-6 relative overflow-hidden h-full flex flex-col justify-between"
            style={{ background: '#1a3c8f' }}
        >
            <div className="absolute -top-5 -right-5 w-[100px] h-[100px] rounded-full bg-white/[0.06]" />
            <div className="absolute -bottom-8 right-20 w-[70px] h-[70px] rounded-full bg-white/[0.04]" />

            <div>
                <p className="text-white/55 text-[11px] tracking-[0.08em] uppercase mb-1">
                    Saldo ogółem
                </p>
                <p className="text-white text-[32px] font-medium mb-1.5 tracking-[-0.5px]">
                    {mainCurrency === 'EUR' ? '€ ' : (mainCurrency === 'PLN' ? '' : mainCurrency + ' ')}
                    {intPart},<span className="text-[22px] opacity-75">{fracPart}</span>
                    {mainCurrency === 'PLN' && <span className="text-[22px] ml-1">zł</span>}
                </p>

                <div className="flex items-center gap-2 mb-5">
                    <div className="bg-green-400/20 rounded-[5px] px-1.5 py-0.5 flex items-center gap-1">
                        <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                            <polyline points="18 15 12 9 6 15" stroke="#4ade80" strokeWidth="2.5" strokeLinecap="round" />
                        </svg>
                        <span className="text-green-400 text-[11px] font-medium">+0.0%</span>
                    </div>
                    <span className="text-white/40 text-[11px]">vs. ubiegły miesiąc</span>
                </div>
            </div>

            <div className="grid grid-cols-3 gap-2 mt-auto">
                {accountCards.map(({ label, value, id }) => (
                    <div key={id} className="bg-white/10 rounded-[10px] p-2.5">
                        <p className="text-white/50 text-[10px] mb-0.5 whitespace-nowrap overflow-hidden text-ellipsis">{label}</p>
                        <p className="text-white text-[14px] font-medium whitespace-nowrap overflow-hidden text-ellipsis">{value}</p>
                    </div>
                ))}
                {accountCards.length === 0 && (
                     <div className="bg-white/10 rounded-[10px] p-2.5 col-span-3 text-center">
                         <p className="text-white/50 text-[12px]">Brak rachunków</p>
                     </div>
                )}
            </div>
        </div>
    )
}
