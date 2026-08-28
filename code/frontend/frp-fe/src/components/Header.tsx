import { useTranslation } from 'react-i18next'
import type { UserDto } from '../api/models/UserDto'
import { HeaderTitle, LinkText } from './UIComponent/Text.tsx'
import { Button } from 'flowbite-react'
import { useState } from 'react'
import { UserManagementService } from '../api/services/UserManagementService'
import { Paths } from '../constants/Paths'

type HeaderProps = {
  user?: UserDto | null
  onLogout?: () => void
  onMenuToggle?: () => void
}

export default function Header({ user, onLogout, onMenuToggle }: Readonly<HeaderProps>) {
  const { t } = useTranslation()
  const { i18n } = useTranslation()
  const [loading, setLoading] = useState(false)

  const handleLanguageChange = (lng: string) => {
    i18n.changeLanguage(lng)
    localStorage.setItem('lang', lng)
  }

  const onLogoutClick = async () => {
    setLoading(true)
    try {
      await UserManagementService.logout()
      localStorage.removeItem('jwt')
      if (onLogout) {
        onLogout()
      }
      window.location.href = Paths.HOME
    } catch (err) {
      console.error('Logout failed', err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <header className="sticky top-0 z-20 flex min-h-16 items-center gap-3 border-b border-gray-200 bg-white px-4 py-3 shadow-sm md:px-6">
      {user && (
        <button
          type="button"
          aria-label={t('navigation.openMenu')}
          aria-controls="application-navigation"
          className="rounded-md border border-gray-300 p-2 text-gray-700 hover:bg-gray-100 md:hidden"
          onClick={onMenuToggle}
        >
          <svg aria-hidden="true" viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor">
            <path strokeLinecap="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
      )}
      <div className="min-w-0 flex-1 text-left">
        <LinkText to={Paths.HOME}>
          <HeaderTitle>
            <span className="sm:hidden">FRP</span>
            <span className="hidden sm:inline">Family Resource Planner</span>
          </HeaderTitle>
        </LinkText>
      </div>
      <div>
        <div className="flex flex-wrap items-center justify-end gap-2 md:gap-4">
          <div>
            <select
              value={i18n.language}
              onChange={(e) => handleLanguageChange(e.target.value)}
              className="text-black px-3 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-400 transition"
            >
              <option value="cs">Čeština</option>
              <option value="en">English</option>
            </select>
          </div>
          <div className="hidden sm:block">{user && <LinkText to={Paths.PROFILE}>{user.fullName}</LinkText>}</div>
          <div className="hidden lg:block">
            <span>{user?.activeSchema}</span>
          </div>
          {user && (
            <div>
              <Button disabled={loading} onClick={onLogoutClick}>
                {loading ? t('logout.button-progress') : t('logout.button')}
              </Button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
