import { render, screen, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import App from './App'
import { UserManagementService } from './api'

// Mock child components
vi.mock('./components/Header', () => ({
  default: () => <div data-testid="header">Header</div>,
}))
vi.mock('./components/Footer', () => ({
  default: () => <div data-testid="footer">Footer</div>,
}))
vi.mock('./AppRoutes', () => ({
  AppRoutes: ({ onLoginSuccess, onRegisterSuccess }: any) => (
    <div data-testid="app-routes">
      <button onClick={() => onLoginSuccess({ user: { id: 1, email: 'test' }, token: 'new-token' })}>Mock Login</button>
      <button onClick={() => onLoginSuccess({ user: null, token: 'token' })}>Mock Login No User</button>
      <button onClick={() => onRegisterSuccess()}>Mock Register</button>
    </div>
  ),
}))

// Mock Service
vi.mock('./api', () => ({
  UserManagementService: {
    authenticatedUser: vi.fn(),
  },
  OpenAPI: { TOKEN: undefined },
}))

// Mock translations (needed if imported indirectly or used)
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'en', changeLanguage: vi.fn() },
  }),
}))

describe('App Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renders spinner while loading user', () => {
    localStorage.setItem('jwt', 'token')
    // Return a promise that never resolves immediately to test loading state
    ;(UserManagementService.authenticatedUser as any).mockImplementation(() => new Promise(() => {}))

    render(<App />)
    expect(screen.getByLabelText('Loading...')).toBeInTheDocument() // Flowbite spinner default aria-label might vary, checking generic
  })

  it('loads user if jwt exists', async () => {
    localStorage.setItem('jwt', 'token')
    const mockUser = { id: 1, email: 'test@example.com' }
    ;(UserManagementService.authenticatedUser as any).mockResolvedValue(mockUser)

    await act(async () => {
      render(<App />)
    })

    await waitFor(() => {
      expect(UserManagementService.authenticatedUser).toHaveBeenCalled()
      expect(screen.getByTestId('header')).toBeInTheDocument()
      expect(screen.getByTestId('app-routes')).toBeInTheDocument()
      expect(screen.getByTestId('footer')).toBeInTheDocument()
    })
  })

  it('renders without user if no jwt', async () => {
    await act(async () => {
      render(<App />)
    })

    expect(UserManagementService.authenticatedUser).not.toHaveBeenCalled()
    expect(screen.getByTestId('header')).toBeInTheDocument()
  })

  it('handles login success', async () => {
    await act(async () => {
      render(<App />)
    })

    const loginBtn = screen.getByText('Mock Login')
    fireEvent.click(loginBtn)

    await waitFor(() => {
      expect(localStorage.getItem('jwt')).toBe('new-token')
    })
  })

  it('handles user load failure', async () => {
    localStorage.setItem('jwt', 'invalid-token')
    ;(UserManagementService.authenticatedUser as any).mockRejectedValue(new Error('Auth failed'))

    await act(async () => {
      render(<App />)
    })

    await waitFor(() => {
      expect(localStorage.getItem('jwt')).toBeNull()
    })
  })

  it('handles login success without user data (error case)', async () => {
    await act(async () => {
      render(<App />)
    })

    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const loginBtn = screen.getByText('Mock Login No User')
    fireEvent.click(loginBtn)

    expect(consoleSpy).toHaveBeenCalledWith('Login response does not contain user data')
  })

  it('handles register success', async () => {
    const originalLocation = window.location
    delete (window as any).location
    // @ts-expect-error - mock location
    window.location = { ...originalLocation, href: '' }

    await act(async () => {
      render(<App />)
    })

    fireEvent.click(screen.getByText('Mock Register'))
    expect(window.location.href).toBe('/login')

    // @ts-expect-error - restore location
    window.location = originalLocation
  })
})

import { fireEvent } from '@testing-library/react'
