export const AUTH_STORAGE_KEY = "eu-bank-session"

export async function register({ email, password, firstName, lastName, passportNumber }) {
    const response = await fetch("/auth/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password, firstName, lastName, passportNumber }),
    })

    if (!response.ok) {
        let errorMessage = "Nie udało się utworzyć konta. Spróbuj ponownie za chwilę."
        try {
            const errorData = await response.json()
            if (errorData && errorData.message) {
                errorMessage = errorData.message
            } else if (response.status === 409) {
                errorMessage = "Klient z podanym e-mailem lub numerem paszportu już istnieje."
            } else if (response.status === 400) {
                errorMessage = "Sprawdź poprawność danych rejestracyjnych."
            }
        } catch (e) {
            if (response.status === 409) {
                errorMessage = "Klient z podanym e-mailem lub numerem paszportu już istnieje."
            } else if (response.status === 400) {
                errorMessage = "Sprawdź poprawność danych rejestracyjnych."
            }
        }
        throw new Error(errorMessage)
    }

    return response.json()
}

export async function login({ email, password }) {
    const response = await fetch("/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
    })

    if (!response.ok) {
        let errorMessage = "Nie udało się zalogować. Spróbuj ponownie za chwilę."
        try {
            const errorData = await response.json()
            if (errorData && errorData.message) {
                errorMessage = errorData.message
            } else if (response.status === 401) {
                errorMessage = "Nieprawidłowy e-mail lub hasło."
            } else if (response.status === 400) {
                errorMessage = "Sprawdź poprawność e-maila i hasła."
            }
        } catch (e) {
            if (response.status === 401) {
                errorMessage = "Nieprawidłowy e-mail lub hasło."
            } else if (response.status === 400) {
                errorMessage = "Sprawdź poprawność e-maila i hasła."
            }
        }
        throw new Error(errorMessage)
    }

    const data = await response.json()

    return {
        email,
        token: data.token,
        tokenType: data.tokenType ?? "Bearer",
    }
}
