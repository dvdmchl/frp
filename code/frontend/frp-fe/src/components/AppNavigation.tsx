import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { UserDto } from '../api/models/UserDto'
import type { ModuleDefinitionDto } from '../api/models/ModuleDefinitionDto'
import { ModuleManagementService } from '../api/services/ModuleManagementService'
import { Paths } from '../constants/Paths'

type AppNavigationProps = {
  user: UserDto
  mobileOpen: boolean
  onClose: () => void
}

type NavigationLinkProps = {
  to: string
  children: React.ReactNode
  end?: boolean
  onClick?: () => void
}

function NavigationLink({ to, children, end, onClick }: Readonly<NavigationLinkProps>) {
  return (
    <NavLink
      to={to}
      end={end}
      onClick={onClick}
      className={({ isActive }) =>
        `block rounded-md px-3 py-2 text-left text-sm transition-colors hover:bg-blue-100 hover:text-blue-900 ${
          isActive ? 'bg-blue-600 font-medium text-white hover:bg-blue-700 hover:text-white' : 'text-gray-700'
        }`
      }
    >
      {children}
    </NavLink>
  )
}

export function AppNavigation({ user, mobileOpen, onClose }: Readonly<AppNavigationProps>) {
  const { t } = useTranslation()
  const location = useLocation()
  const [modules, setModules] = useState<ModuleDefinitionDto[]>([])

  useEffect(() => {
    ModuleManagementService.listModules()
      .then((data) => setModules(data.filter((module) => module.state === 'ENABLED' && module.code)))
      .catch((error: unknown) => console.error('Failed to load modules', error))
  }, [])

  useEffect(() => {
    onClose()
  }, [location.pathname, onClose])

  const linkClick = mobileOpen ? onClose : undefined

  return (
    <>
      {mobileOpen && (
        <button
          type="button"
          aria-label={t('navigation.closeMenu')}
          className="fixed inset-0 z-30 bg-black/40 md:hidden"
          onClick={onClose}
        />
      )}
      <aside
        id="application-navigation"
        aria-label={t('navigation.main')}
        className={`fixed inset-y-0 left-0 z-40 w-72 overflow-y-auto border-r border-gray-200 bg-white p-4 shadow-xl transition-transform md:static md:z-auto md:w-64 md:translate-x-0 md:shadow-none ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="mb-4 flex items-center justify-between md:hidden">
          <span className="text-lg font-semibold text-gray-900">{t('navigation.menu')}</span>
          <button
            type="button"
            aria-label={t('navigation.closeMenu')}
            className="rounded-md p-2 text-2xl leading-none text-gray-600 hover:bg-gray-100"
            onClick={onClose}
          >
            ×
          </button>
        </div>

        <nav className="space-y-2">
          <NavigationLink to={Paths.HOME} end onClick={linkClick}>
            {t('navigation.home')}
          </NavigationLink>

          <details open={location.pathname.startsWith(Paths.PROFILE)} className="group">
            <summary className="cursor-pointer rounded-md px-3 py-2 text-left font-medium text-gray-900 hover:bg-gray-100">
              {t('navigation.profile')}
            </summary>
            <div className="ml-3 mt-1 space-y-1 border-l border-gray-200 pl-3">
              <NavigationLink to={Paths.PROFILE_PERSONAL_INFO} onClick={linkClick}>
                {t('profile.personalInfo')}
              </NavigationLink>
              <NavigationLink to={Paths.SECURITY} onClick={linkClick}>
                {t('profile.security')}
              </NavigationLink>
              <NavigationLink to={Paths.DATABASE_SCHEMAS} onClick={linkClick}>
                {t('profile.databaseSchemas')}
              </NavigationLink>
            </div>
          </details>

          {user.admin && (
            <details open={location.pathname.startsWith(Paths.ADMIN)} className="group">
              <summary className="cursor-pointer rounded-md px-3 py-2 text-left font-medium text-gray-900 hover:bg-gray-100">
                {t('admin.title')}
              </summary>
              <div className="ml-3 mt-1 space-y-1 border-l border-gray-200 pl-3">
                <NavigationLink to={Paths.ADMIN_USERS} onClick={linkClick}>
                  {t('admin.userManagement.title')}
                </NavigationLink>
                <NavigationLink to={Paths.ADMIN_MAINTENANCE} onClick={linkClick}>
                  {t('admin.maintenance.title')}
                </NavigationLink>
              </div>
            </details>
          )}

          {modules.length > 0 && (
            <details open={location.pathname.startsWith('/modules/')} className="group">
              <summary className="cursor-pointer rounded-md px-3 py-2 text-left font-medium text-gray-900 hover:bg-gray-100">
                {t('modules.menuTitle')}
              </summary>
              <div className="ml-3 mt-1 space-y-1 border-l border-gray-200 pl-3">
                {modules.map((module) => {
                  const modulePath = Paths.MODULES.replace(':moduleCode', module.code ?? '')
                  if (module.code === 'ACC') {
                    return (
                      <details key={module.code} open={location.pathname.startsWith(modulePath)}>
                        <summary className="cursor-pointer rounded-md px-3 py-2 text-left text-sm font-medium text-gray-800 hover:bg-gray-100">
                          {module.title ?? module.code}
                        </summary>
                        <div className="ml-3 mt-1 space-y-1 border-l border-gray-200 pl-3">
                          <NavigationLink to={modulePath} end onClick={linkClick}>
                            {t('account.title')}
                          </NavigationLink>
                          <NavigationLink to={`${modulePath}/${Paths.ACCOUNTING_CURRENCIES}`} onClick={linkClick}>
                            {t('currency.title')}
                          </NavigationLink>
                        </div>
                      </details>
                    )
                  }

                  return (
                    <NavigationLink key={module.code} to={modulePath} onClick={linkClick}>
                      {module.title ?? module.code}
                    </NavigationLink>
                  )
                })}
              </div>
            </details>
          )}
        </nav>
      </aside>
    </>
  )
}
