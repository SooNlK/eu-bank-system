import { useState, useEffect } from 'react';

export default function BalanceCard({ accounts = [], loading }) {
    const [selectedId, setSelectedId] = useState(null);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        if (accounts && accounts.length > 0 && !selectedId) {
            setSelectedId(accounts[0].id);
        }
    }, [accounts, selectedId]);

    const selectedAccount = accounts.find(acc => acc.id === selectedId) || accounts[0];

    const formatIBAN = (iban) => {
        if (!iban) return '';
        return iban.replace(/\s+/g, '').replace(/(.{4})/g, '$1 ').trim();
    };

    const handleCopy = (text) => {
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

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

                <div className="flex items-center gap-2 mb-4">
                    <div className="bg-green-400/20 rounded-[5px] px-1.5 py-0.5 flex items-center gap-1">
                        <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                            <polyline points="18 15 12 9 6 15" stroke="#4ade80" strokeWidth="2.5" strokeLinecap="round" />
                        </svg>
                        <span className="text-green-400 text-[11px] font-medium">+0.0%</span>
                    </div>
                    <span className="text-white/40 text-[11px]">vs. ubiegły miesiąc</span>
                </div>
            </div>

            {/* Szegóły wybranego konta (IBAN, typ, status) */}
            {selectedAccount && (
                <div className="bg-white/10 border border-white/5 rounded-[12px] p-3.5 my-3 flex items-center justify-between backdrop-blur-sm transition-all duration-200">
                    <div className="min-w-0 flex-1 mr-3 text-left">
                        <div className="flex items-center gap-1.5 mb-1">
                            <span className="text-[10px] font-semibold text-white/60 uppercase tracking-[0.05em]">
                                {selectedAccount.type === 'STANDARD' ? 'Rachunek główny' : selectedAccount.type}
                            </span>
                            <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
                            <span className="text-[9px] text-green-400 font-medium uppercase tracking-[0.02em]">Aktywne</span>
                        </div>
                        <p className="text-white text-[13px] font-mono tracking-wider font-semibold truncate select-all">
                            {formatIBAN(selectedAccount.accountNumber)}
                        </p>
                    </div>
                    <button
                        onClick={() => handleCopy(selectedAccount.accountNumber)}
                        className="bg-white/10 hover:bg-white/20 border-none rounded-lg p-2 cursor-pointer text-white flex items-center justify-center transition-all duration-200 hover:scale-105 shrink-0"
                        title="Skopiuj numer konta"
                    >
                        {copied ? (
                            <div className="flex items-center gap-1">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#4ade80" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <span className="text-[10px] text-green-400 font-medium">Skopiowano</span>
                            </div>
                        ) : (
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                            </svg>
                        )}
                    </button>
                </div>
            )}

        </div>
    )
}
