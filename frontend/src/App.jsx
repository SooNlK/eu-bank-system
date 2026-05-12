import { useState } from "react"
import Dashboard from "./components/Dashboard"
import LoginPage from "./components/LoginPage"
import { AUTH_STORAGE_KEY } from "./services/auth"


function App() {
  const [session, setSession] = useState(() => {
    const savedSession = localStorage.getItem(AUTH_STORAGE_KEY)

    if (!savedSession) {
      return null
    }

    try {
      return JSON.parse(savedSession)
    } catch {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      return null
    }
  })

  function handleLogin(nextSession) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession))
    setSession(nextSession)
  }

  function handleLogout() {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    setSession(null)
  }

  return (
    session ? (
      <Dashboard userEmail={session.email} onLogout={handleLogout} />
    ) : (
      <LoginPage onLogin={handleLogin} />
    )
  )
}

export default App
