import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { RegisterForm } from './RegisterForm';
import { UserManagementService } from '../../api/services/UserManagementService';
import { ApiError } from '../../api/core/ApiError';
import type { ErrorDto } from '../../api/models/ErrorDto';

// Mock translations
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

// Mock Service
vi.mock('../../api/services/UserManagementService', () => ({
  UserManagementService: {
    register: vi.fn(),
  },
}));

describe('RegisterForm Component', () => {
  const mockOnRegisterSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders form elements', () => {
    render(<RegisterForm onRegisterSuccess={mockOnRegisterSuccess} />);
    expect(screen.getByText('register.title')).toBeInTheDocument();
    expect(screen.getByLabelText('register.fullName')).toBeInTheDocument();
    expect(screen.getByLabelText('login.email')).toBeInTheDocument();
    expect(screen.getByLabelText('login.password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'register.button' })).toBeInTheDocument();
  });

  it('handles successful registration', async () => {
    const mockResponse = { id: 1, email: 'test@example.com', fullName: 'Test User' };
    (UserManagementService.register as any).mockResolvedValue(mockResponse);

    render(<RegisterForm onRegisterSuccess={mockOnRegisterSuccess} />);

    fireEvent.change(screen.getByLabelText('register.fullName'), { target: { value: 'Test User' } });
    fireEvent.change(screen.getByLabelText('login.email'), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText('login.password'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: 'register.button' }));

    await waitFor(() => {
      expect(UserManagementService.register).toHaveBeenCalledWith({ fullName: 'Test User', email: 'test@example.com', password: 'password' });
      expect(mockOnRegisterSuccess).toHaveBeenCalledWith(mockResponse);
    });
  });

  it('handles registration failure', async () => {
     const errorDto: ErrorDto = { message: 'Registration failed' };
     
     const request = { method: 'POST', url: '/register' };
     const response = {
        url: '/register',
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        body: errorDto,
     };
     const apiError = new ApiError(request as any, response as any, 'Bad Request');

    (UserManagementService.register as any).mockRejectedValue(apiError);

    render(<RegisterForm onRegisterSuccess={mockOnRegisterSuccess} />);

    fireEvent.change(screen.getByLabelText('register.fullName'), { target: { value: 'Test User' } });
    fireEvent.change(screen.getByLabelText('login.email'), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText('login.password'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: 'register.button' }));

    await waitFor(() => {
      expect(screen.getByText('Registration failed')).toBeInTheDocument();
    });
  });
});
