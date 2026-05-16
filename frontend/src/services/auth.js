export const AUTH_STORAGE_KEY = "eu-bank-session"

export async function login({ email, password }) {
    const response = await fetch("/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
    })

    if (!response.ok) {
        if (response.status === 401) {
            throw new Error("Nieprawidłowy e-mail lub hasło.")
        }

        if (response.status === 400) {
            throw new Error("Sprawdź poprawność e-maila i hasła.")
        }

        throw new Error("Nie udało się zalogować. Spróbuj ponownie za chwilę.")
    }

    const data = await response.json()

    return {
        email,
        token: data.token,
        tokenType: data.tokenType ?? "Bearer",
    }
}
