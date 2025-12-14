import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { MemoryRouter } from 'react-router-dom';

// Mock components
vi.mock('./components/UserManagement/LoginForm', () => ({
  LoginForm: () => <div data-testid="login-form">Login Form</div>,
}));
vi.mock('./components/UserManagement/RegisterForm', () => ({
  RegisterForm: () => <div data-testid="register-form">Register Form</div>,
}));
vi.mock('./components/UserManagement/ProfileEditForm', () => ({
  ProfileEditForm: () => <div data-testid="profile-form">Profile Form</div>,
}));
vi.mock('./components/HomePage', () => ({
  HomePage: () => <div data-testid="home-page">Home Page</div>,
}));

describe('AppRoutes Component', () => {
  const mockSetUser = vi.fn();
  const mockOnLoginSuccess = vi.fn();
  const mockOnRegisterSuccess = vi.fn();

  it('redirects to login when no user', () => {
    render(
      <MemoryRouter initialEntries={['/random']}>
        <AppRoutes 
            user={null} 
            onLoginSuccess={mockOnLoginSuccess} 
            onRegisterSuccess={mockOnRegisterSuccess} 
            setUser={mockSetUser} 
        />
      </MemoryRouter>
    );
    expect(screen.getByTestId('login-form')).toBeInTheDocument();
  });

  it('renders register route when no user', () => {
    render(
      <MemoryRouter initialEntries={['/register']}>
        <AppRoutes 
            user={null} 
            onLoginSuccess={mockOnLoginSuccess} 
            onRegisterSuccess={mockOnRegisterSuccess} 
            setUser={mockSetUser} 
        />
      </MemoryRouter>
    );
    expect(screen.getByTestId('register-form')).toBeInTheDocument();
  });

  it('redirects to home when user exists', () => {
    const user = { id: 1, email: 'test', fullName: 'name' };
    render(
      <MemoryRouter initialEntries={['/random']}>
        <AppRoutes 
            user={user} 
            onLoginSuccess={mockOnLoginSuccess} 
            onRegisterSuccess={mockOnRegisterSuccess} 
            setUser={mockSetUser} 
        />
      </MemoryRouter>
    );
    expect(screen.getByTestId('home-page')).toBeInTheDocument();
  });

  it('renders profile when user exists', () => {
    const user = { id: 1, email: 'test', fullName: 'name' };
    render(
      <MemoryRouter initialEntries={['/profile/info']}>
        <AppRoutes 
            user={user} 
            onLoginSuccess={mockOnLoginSuccess} 
            onRegisterSuccess={mockOnRegisterSuccess} 
            setUser={mockSetUser} 
        />
      </MemoryRouter>
    );
    expect(screen.getByTestId('profile-form')).toBeInTheDocument();
  });
});
