import { useState } from "react"
import { login } from "../services/auth"
import heroImage from "../assets/hero.png"

export default function LoginPage({ onLogin }) {
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [error, setError] = useState("")
    const [isSubmitting, setIsSubmitting] = useState(false)

    async function handleSubmit(event) {
        event.preventDefault()
        setError("")

        if (!email.trim() || password.length < 8) {
            setError("Podaj poprawny e-mail i hasło składające się z co najmniej 8 znaków.")
            return
        }

        setIsSubmitting(true)

        try {
            const session = await login({ email: email.trim(), password })
            onLogin(session)
        } catch (loginError) {
            setError(loginError.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main className="min-h-screen bg-[#eef3fb] text-slate-950">
            <div className="grid min-h-screen lg:grid-cols-[1.05fr_0.95fr]">
                <section className="relative hidden overflow-hidden bg-[#163b87] lg:block">
                    <img
                        src={heroImage}
                        alt=""
                        className="absolute inset-0 h-full w-full object-cover opacity-90"
                    />
                    <div className="absolute inset-0 bg-gradient-to-r from-[#0b2255]/95 via-[#12367d]/78 to-[#12367d]/22" />
                    <div className="relative flex h-full flex-col justify-between p-10">
                        <div className="flex items-center gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/15">
                                <GlobeIcon />
                            </div>
                            <div>
                                <p className="text-[17px] font-semibold text-white">EuroBank</p>
                                <p className="text-[12px] text-white/55">Private Banking</p>
                            </div>
                        </div>

                        <div className="max-w-[520px] pb-8">
                            <p className="mb-4 text-[12px] font-medium uppercase tracking-[0.14em] text-blue-100/70">
                                Bezpieczny dostęp
                            </p>
                            <h1 className="text-[44px] font-semibold leading-tight tracking-normal text-white">
                                Zaloguj się do panelu bankowego.
                            </h1>
                            <p className="mt-5 max-w-[420px] text-[15px] leading-7 text-blue-50/78">
                                Sprawdź rachunki, historię operacji i przygotuj przelew w jednym spokojnym widoku.
                            </p>
                        </div>
                    </div>
                </section>

                <section className="flex min-h-screen items-center justify-center px-5 py-8 sm:px-8">
                    <div className="w-full max-w-[430px]">
                        <div className="mb-8 flex items-center gap-3 lg:hidden">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-[#1a3c8f]">
                                <GlobeIcon />
                            </div>
                            <div>
                                <p className="text-[17px] font-semibold text-slate-950">EuroBank</p>
                                <p className="text-[12px] text-slate-500">Private Banking</p>
                            </div>
                        </div>

                        <div className="rounded-lg border border-slate-200 bg-white px-6 py-7 shadow-sm sm:px-8">
                            <div className="mb-6">
                                <p className="text-[13px] font-medium text-[#1a3c8f]">Logowanie</p>
                                <h2 className="mt-1 text-[27px] font-semibold tracking-normal text-slate-950">
                                    Witaj ponownie
                                </h2>
                            </div>

                            <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
                                <label className="flex flex-col gap-1.5">
                                    <span className="text-[13px] font-medium text-slate-700">E-mail</span>
                                    <input
                                        type="email"
                                        value={email}
                                        onChange={(event) => setEmail(event.target.value)}
                                        autoComplete="email"
                                        placeholder="jan.kowalski@example.com"
                                        className="h-11 rounded-lg border border-slate-200 bg-slate-50 px-3 text-[14px] text-slate-900 outline-none transition focus:border-[#2563eb] focus:bg-white focus:ring-4 focus:ring-blue-100"
                                    />
                                </label>

                                <label className="flex flex-col gap-1.5">
                                    <span className="text-[13px] font-medium text-slate-700">Hasło</span>
                                    <input
                                        type="password"
                                        value={password}
                                        onChange={(event) => setPassword(event.target.value)}
                                        autoComplete="current-password"
                                        placeholder="Minimum 8 znaków"
                                        className="h-11 rounded-lg border border-slate-200 bg-slate-50 px-3 text-[14px] text-slate-900 outline-none transition focus:border-[#2563eb] focus:bg-white focus:ring-4 focus:ring-blue-100"
                                    />
                                </label>

                                {error && (
                                    <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-[13px] leading-5 text-red-700">
                                        {error}
                                    </div>
                                )}

                                <button
                                    type="submit"
                                    disabled={isSubmitting}
                                    className="mt-1 flex h-11 items-center justify-center gap-2 rounded-lg border-none bg-[#1a3c8f] px-4 text-[14px] font-semibold text-white transition hover:bg-[#163579] disabled:cursor-not-allowed disabled:bg-slate-300"
                                >
                                    {isSubmitting ? "Logowanie..." : "Zaloguj się"}
                                    {!isSubmitting && <ArrowIcon />}
                                </button>
                            </form>
                        </div>
                    </div>
                </section>
            </div>
        </main>
    )
}

function GlobeIcon() {
    return (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="white" strokeWidth="1.8" />
            <path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10A15.3 15.3 0 0 1 8 12 15.3 15.3 0 0 1 12 2z" stroke="white" strokeWidth="1.8" />
        </svg>
    )
}

function ArrowIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    )
}
