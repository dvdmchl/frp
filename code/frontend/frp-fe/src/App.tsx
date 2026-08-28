import './App.css'
import { BrowserRouter } from 'react-router-dom'
import { useCallback, useState, useEffect } from 'react'
import type { UserDto } from './api/models/UserDto.ts'
import type { UserLoginResponseDto } from './api/models/UserLoginResponseDto'
import Header from './components/Header'
import Footer from './components/Footer'
import { OpenAPI, UserManagementService } from './api'
import { AppRoutes } from './AppRoutes.tsx'
import { Spinner } from 'flowbite-react'
import { AppNavigation } from './components/AppNavigation'

import { Paths } from './constants/Paths'

function App() {
  const [user, setUser] = useState<UserDto | null>(null)
  const [loadingUser, setLoadingUser] = useState(true)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const closeMobileMenu = useCallback(() => setMobileMenuOpen(false), [])
  const toggleMobileMenu = useCallback(() => setMobileMenuOpen((open) => !open), [])

  useEffect(() => {
    const jwt = localStorage.getItem('jwt')
    if (jwt) {
      OpenAPI.TOKEN = jwt
      UserManagementService.authenticatedUser()
        .then((u) => setUser(u))
        .catch(() => {
          setUser(null)
          localStorage.removeItem('jwt')
          OpenAPI.TOKEN = undefined
        })
        .finally(() => {
          setLoadingUser(false)
        })
    } else {
      setLoadingUser(false)
    }
  }, [])

  if (loadingUser) {
    return <Spinner aria-label="Loading..." />
  }

  const handleLoginSuccess = (loginResp: UserLoginResponseDto) => {
    if (!loginResp.user) {
      console.error('Login response does not contain user data')
      return
    }
    setUser(loginResp.user)
    if (loginResp.token != null) {
      localStorage.setItem('jwt', loginResp.token)
      OpenAPI.TOKEN = loginResp.token
    }
  }

  const handleRegisterSuccess = () => {
    // Po registraci můžeš:
    // - přesměrovat na login
    // - nebo rovnou přihlásit (pokud ti /register vrací uživatele+token)
    window.location.href = Paths.LOGIN // nebo useNavigate("/login")
  }

  return (
    <div className="min-h-screen flex flex-col">
      <BrowserRouter>
        <Header user={user} onLogout={() => setUser(null)} onMenuToggle={toggleMobileMenu} />
        <div className="flex min-h-0 flex-grow">
          {user && <AppNavigation user={user} mobileOpen={mobileMenuOpen} onClose={closeMobileMenu} />}
          <main className="min-w-0 flex-grow p-4 md:p-6">
            <AppRoutes
              user={user}
              onLoginSuccess={handleLoginSuccess}
              onRegisterSuccess={handleRegisterSuccess}
              setUser={setUser}
            />
          </main>
        </div>
        <Footer />
      </BrowserRouter>
    </div>
  )
}

export default App
