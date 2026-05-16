import { AUTH_STORAGE_KEY } from "./auth"

function getHeaders() {
    const sessionStr = localStorage.getItem(AUTH_STORAGE_KEY)
    let headers = {
        "Content-Type": "application/json",
    }
    if (sessionStr) {
        try {
            const session = JSON.parse(sessionStr)
            if (session.token) {
                headers["Authorization"] = `Bearer ${session.token}`
            }
        } catch (e) {
            console.error("Failed to parse session", e)
        }
    }
    return headers
}

export async function getMyAccounts() {
    const response = await fetch("/api/accounts", {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error("Failed to fetch accounts")
    }
    return response.json()
}

export async function getAccountTransactions(accountId) {
    const response = await fetch(`/api/accounts/${accountId}/transactions`, {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error("Failed to fetch transactions")
    }
    return response.json()
}
