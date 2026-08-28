import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppNavigation } from './AppNavigation'
import { ModuleManagementService } from '../api/services/ModuleManagementService'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

describe('AppNavigation', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the complete tree for an administrator', async () => {
    vi.spyOn(ModuleManagementService, 'listModules').mockResolvedValue([
      { code: 'ACC', title: 'Accounting', state: 'ENABLED' },
      { code: 'DISABLED', title: 'Disabled module', state: 'DISABLED' },
    ])

    render(
      <MemoryRouter initialEntries={['/modules/ACC']}>
        <AppNavigation user={{ id: 1, email: 'admin@example.com', admin: true }} mobileOpen={false} onClose={vi.fn()} />
      </MemoryRouter>,
    )

    expect(screen.getByRole('complementary', { name: 'navigation.main' })).toBeInTheDocument()
    expect(screen.getByText('navigation.profile')).toBeInTheDocument()
    expect(screen.getByText('admin.title')).toBeInTheDocument()

    await waitFor(() => expect(screen.getByText('Accounting')).toBeInTheDocument())
    expect(screen.getByRole('link', { name: 'account.title' })).toHaveAttribute('href', '/modules/ACC')
    expect(screen.getByRole('link', { name: 'currency.title' })).toHaveAttribute('href', '/modules/ACC/currencies')
    expect(screen.queryByText('Disabled module')).not.toBeInTheDocument()
  })

  it('hides administration from non-admin users', async () => {
    vi.spyOn(ModuleManagementService, 'listModules').mockResolvedValue([])

    render(
      <MemoryRouter>
        <AppNavigation user={{ id: 2, email: 'user@example.com' }} mobileOpen={false} onClose={vi.fn()} />
      </MemoryRouter>,
    )

    expect(screen.queryByText('admin.title')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'navigation.home' })).toBeInTheDocument()
  })

  it('closes the mobile drawer after selecting a destination', () => {
    vi.spyOn(ModuleManagementService, 'listModules').mockResolvedValue([])
    const onClose = vi.fn()

    render(
      <MemoryRouter initialEntries={['/profile/personal-info']}>
        <AppNavigation user={{ id: 2, email: 'user@example.com' }} mobileOpen onClose={onClose} />
      </MemoryRouter>,
    )

    onClose.mockClear()
    fireEvent.click(screen.getByRole('link', { name: 'profile.security' }))
    expect(onClose).toHaveBeenCalled()
  })
})
