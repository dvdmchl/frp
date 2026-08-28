import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import Header from './Header'
import { MemoryRouter } from 'react-router-dom'
import { UserManagementService } from '../api/services/UserManagementService'
import type { UserDto } from '../api/models/UserDto'

// Mock dependencies
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: {
      language: 'en',
      changeLanguage: vi.fn(),
    },
  }),
}))

vi.mock('../api/services/UserManagementService', () => ({
  UserManagementService: {
    logout: vi.fn(),
  },
}))

describe('Header Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders title', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    )
    expect(screen.getByText('Family Resource Planner')).toBeInTheDocument()
    expect(screen.getByText('FRP')).toBeInTheDocument()
  })

  it('renders user info when user is provided', () => {
    const user: UserDto = { id: 1, email: 'test@example.com', fullName: 'Test User', activeSchema: 'schema1' }
    render(
      <MemoryRouter>
        <Header user={user} />
      </MemoryRouter>,
    )
    expect(screen.getByText('Test User')).toBeInTheDocument()
    expect(screen.getByText('schema1')).toBeInTheDocument()
  })

  it('opens navigation from the mobile menu button', () => {
    const onMenuToggle = vi.fn()
    const user: UserDto = { id: 1, email: 'test@example.com' }
    render(
      <MemoryRouter>
        <Header user={user} onMenuToggle={onMenuToggle} />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'navigation.openMenu' }))
    expect(onMenuToggle).toHaveBeenCalledOnce()
  })

  it('calls logout and redirects on logout button click', async () => {
    const onLogout = vi.fn()
    // Mock window.location.href
    const originalLocation = window.location
    delete (window as any).location
    window.location = { href: '' } as any

    render(
      <MemoryRouter>
        <Header user={{ id: 1, email: 'test@example.com' }} onLogout={onLogout} />
      </MemoryRouter>,
    )

    const logoutButton = screen.getByRole('button', { name: 'logout.button' })
    fireEvent.click(logoutButton)

    await waitFor(() => {
      expect(UserManagementService.logout).toHaveBeenCalled()
      expect(onLogout).toHaveBeenCalled()
      expect(window.location.href).toBe('/')
    })

    // Cleanup
    window.location = originalLocation as any
  })
})
