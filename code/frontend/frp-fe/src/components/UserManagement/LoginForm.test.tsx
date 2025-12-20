import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { LoginForm } from './LoginForm'
import { UserManagementService } from '../../api/services/UserManagementService'
import { ApiError } from '../../api/core/ApiError'
import type { ErrorDto } from '../../api/models/ErrorDto'

// Mock translations
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

// Mock Service
vi.mock('../../api/services/UserManagementService', () => ({
  UserManagementService: {
    login: vi.fn(),
  },
}))

describe('LoginForm Component', () => {
  const mockOnLoginSuccess = vi.fn()
  const mockOnRegisterClick = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renders form elements', () => {
    render(<LoginForm onLoginSuccess={mockOnLoginSuccess} onRegisterClick={mockOnRegisterClick} />)
    expect(screen.getByText('login.title')).toBeInTheDocument()
    expect(screen.getByLabelText('login.email')).toBeInTheDocument()
    expect(screen.getByLabelText('login.password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'login.button' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'register.title' })).toBeInTheDocument()
  })

  it('handles successful login', async () => {
    const mockResponse = { token: 'fake-token', user: { id: 1, email: 'test@example.com' } }
    ;(UserManagementService.login as any).mockResolvedValue(mockResponse)

    render(<LoginForm onLoginSuccess={mockOnLoginSuccess} onRegisterClick={mockOnRegisterClick} />)

    fireEvent.change(screen.getByLabelText('login.email'), { target: { value: 'test@example.com' } })
    fireEvent.change(screen.getByLabelText('login.password'), { target: { value: 'password' } })
    fireEvent.click(screen.getByRole('button', { name: 'login.button' }))

    await waitFor(() => {
      expect(UserManagementService.login).toHaveBeenCalledWith({ email: 'test@example.com', password: 'password' })
      expect(localStorage.getItem('jwt')).toBe('fake-token')
      expect(mockOnLoginSuccess).toHaveBeenCalledWith(mockResponse)
    })
  })

  it('handles login failure with ApiError', async () => {
    const errorDto: ErrorDto = { message: 'Invalid credentials' }

    const request = { method: 'POST', url: '/login' }
    const response = {
      url: '/login',
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      body: errorDto,
    }
    const apiError = new ApiError(request as any, response as any, 'Unauthorized')

    ;(UserManagementService.login as any).mockRejectedValue(apiError)

    render(<LoginForm onLoginSuccess={mockOnLoginSuccess} onRegisterClick={mockOnRegisterClick} />)

    fireEvent.change(screen.getByLabelText('login.email'), { target: { value: 'test@example.com' } })
    fireEvent.change(screen.getByLabelText('login.password'), { target: { value: 'wrong' } })
    fireEvent.click(screen.getByRole('button', { name: 'login.button' }))

    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument()
    })
  })

  it('calls onRegisterClick when register button is clicked', () => {
    render(<LoginForm onLoginSuccess={mockOnLoginSuccess} onRegisterClick={mockOnRegisterClick} />)
    fireEvent.click(screen.getByRole('button', { name: 'register.title' }))
    expect(mockOnRegisterClick).toHaveBeenCalled()
  })
})
