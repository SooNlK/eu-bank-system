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

async function readError(response) {
    try {
        const body = await response.json()
        // Backend may return { error: { message: "..." } } or { message: "..." }
        return (body.error && body.error.message) || body.message || `Błąd serwera: ${response.status} ${response.statusText}`
    } catch {
        return `Błąd serwera: ${response.status} ${response.statusText}`
    }
}

// C2B - Kod BLIK / KLIK
export async function generateKlikCode(accountId) {
    const response = await fetch("/api/blik/generate", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({ accountId }),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function getKlikPendingTransactions() {
    const response = await fetch("/api/blik/pending", {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function confirmKlikTransaction(transactionId, status) {
    const response = await fetch("/api/blik/confirm", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({ transactionId, status }),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response
}

// P2P - Przelewy na telefon
export async function registerKlikAlias(accountId, phone) {
    const response = await fetch("/api/blik/p2p/register", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({ accountId, phone }),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function unregisterKlikAlias(phone) {
    const response = await fetch(`/api/blik/p2p/unregister?phone=${encodeURIComponent(phone)}`, {
        method: "DELETE",
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response
}

export async function createKlikP2pTransfer({ fromAccountId, toPhone, amount, currency, description }) {
    const response = await fetch("/api/blik/p2p/transfer", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({ fromAccountId, toPhone, amount, currency, description }),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}
