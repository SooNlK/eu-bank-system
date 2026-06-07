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

export async function getTransactionDetails(accountId, transactionId) {
    const response = await fetch(`/api/accounts/${accountId}/transactions/${transactionId}`, {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error("Failed to fetch transaction details")
    }
    return response.json()
}

async function readError(response) {
    try {
        const body = await response.json()
        return body.message || "Request failed"
    } catch {
        return "Request failed"
    }
}

export async function createInternalTransfer({ fromAccountId, toIban, amount, currency, valueDate, description }) {
    const response = await fetch("/api/transfers", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({
            fromAccountId,
            toIban,
            amount,
            currency,
            valueDate,
            channel: "INTERNAL",
            description,
        }),
    })

    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

/**
 * Wysyła przelew zewnętrzny (SEPA / SEPA_INSTANT / TARGET) do symulatora EU Payments.
 * @param {Object} opts
 * @param {string} opts.fromAccountId - UUID rachunku źródłowego
 * @param {string} opts.toIban        - IBAN odbiorcy
 * @param {string} opts.toBic         - BIC banku odbiorcy (wymagany dla TARGET)
 * @param {string} opts.beneficiaryName - Nazwa odbiorcy
 * @param {number} opts.amount        - Kwota
 * @param {string} opts.currency      - Waluta (np. "EUR")
 * @param {string} opts.channel       - "SEPA" | "SEPA_INSTANT" | "TARGET"
 * @param {string} [opts.valueDate]   - Data waluty (ISO 8601, opcjonalna)
 * @param {string} [opts.description] - Tytuł przelewu
 */
export async function createExternalTransfer({
    fromAccountId,
    toIban,
    toBic,
    beneficiaryName,
    amount,
    currency,
    channel,
    valueDate,
    description,
}) {
    const response = await fetch("/api/transfers", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({
            fromAccountId,
            toIban,
            toBic: toBic || null,
            beneficiaryName: beneficiaryName || null,
            amount,
            currency: currency || "EUR",
            channel,
            valueDate: valueDate || null,
            description: description || null,
        }),
    })

    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}


export async function getJuniorAccounts() {
    const response = await fetch("/api/accounts/junior", {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function createJuniorAccount(data) {
    const response = await fetch("/api/accounts/junior", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify(data),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function getPendingApprovals() {
    const response = await fetch("/api/transfers/pending-approval", {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function approveTransfer(transferId) {
    const response = await fetch(`/api/transfers/${transferId}/approve`, {
        method: "POST",
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function rejectTransfer(transferId) {
    const response = await fetch(`/api/transfers/${transferId}/reject`, {
        method: "POST",
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function getTransfers() {
    const response = await fetch("/api/transfers", {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function getCards() {
    const response = await fetch("/api/cards", {
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function issueCard(data) {
    const response = await fetch("/api/cards", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify(data),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function activateCard(cardId) {
    const response = await fetch(`/api/cards/${cardId}/activate`, {
        method: "POST",
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function blockCard(cardId) {
    const response = await fetch(`/api/cards/${cardId}/block`, {
        method: "POST",
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function unblockCard(cardId) {
    const response = await fetch(`/api/cards/${cardId}/unblock`, {
        method: "POST",
        headers: getHeaders(),
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

export async function updateCardLimits(cardId, dailyLimit, monthlyLimit) {
    const response = await fetch(`/api/cards/${cardId}/limits`, {
        method: "PATCH",
        headers: getHeaders(),
        body: JSON.stringify({
            dailyLimit: Number(dailyLimit),
            monthlyLimit: Number(monthlyLimit)
        })
    })
    if (!response.ok) {
        throw new Error(await readError(response))
    }
    return response.json()
}

