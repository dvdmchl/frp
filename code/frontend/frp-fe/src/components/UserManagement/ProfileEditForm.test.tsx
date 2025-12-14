import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProfileEditForm } from './ProfileEditForm';
import { UserManagementService } from '../../api/services/UserManagementService';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

// Mock translations
const mockT = (key: string) => key;
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: mockT,
  }),
}));

// Mock Service
vi.mock('../../api/services/UserManagementService', () => ({
  UserManagementService: {
    authenticatedUser: vi.fn(),
    updateAuthenticatedUserInfo: vi.fn(),
    changeAuthenticatedUserPassword: vi.fn(),
  },
}));

// Mock SchemaManager
vi.mock('./SchemaManager', () => ({
  SchemaManager: () => <div data-testid="schema-manager">Schema Manager</div>,
}));

describe('ProfileEditForm Component', () => {
  const mockUser = { id: 1, email: 'test@example.com', fullName: 'Test User' };

  beforeEach(() => {
    vi.clearAllMocks();
    (UserManagementService.authenticatedUser as any).mockResolvedValue(mockUser);
  });

  const renderComponent = (section = 'info') => {
    return render(
      <MemoryRouter initialEntries={[`/profile/${section}`]}>
        <Routes>
            <Route path="/profile/:section" element={<ProfileEditForm />} />
             {/* Fallback for no section param */}
            <Route path="*" element={<ProfileEditForm />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('loads user data and renders info form by default', async () => {
    renderComponent('personal-info');

    await waitFor(() => {
      expect(screen.getByDisplayValue('Test User')).toBeInTheDocument();
      expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument();
    });
    expect(screen.getByText('profile.title')).toBeInTheDocument();
  });

  it('updates personal info', async () => {
    (UserManagementService.updateAuthenticatedUserInfo as any).mockResolvedValue({ ...mockUser, fullName: 'New Name' });
    renderComponent('personal-info');

    await waitFor(() => screen.getByDisplayValue('Test User'));

    fireEvent.change(screen.getByLabelText('profile.fullName'), { target: { value: 'New Name' } });
    fireEvent.click(screen.getByRole('button', { name: 'profile.button' }));

    await waitFor(() => {
      expect(UserManagementService.updateAuthenticatedUserInfo).toHaveBeenCalledWith({ fullName: 'New Name', email: 'test@example.com' });
      expect(screen.getByText('profile.infoSuccess')).toBeInTheDocument();
    });
  });

  it('renders security form and updates password', async () => {
    renderComponent('security');

    await waitFor(() => screen.getByRole('heading', { name: 'profile.security' }));

    fireEvent.change(screen.getByLabelText('profile.oldPassword'), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText('profile.password'), { target: { value: 'newPass' } });
    fireEvent.change(screen.getByLabelText('profile.confirmPassword'), { target: { value: 'newPass' } });

    fireEvent.click(screen.getByRole('button', { name: 'profile.button' }));

    await waitFor(() => {
      expect(UserManagementService.changeAuthenticatedUserPassword).toHaveBeenCalledWith({ oldPassword: 'oldPass', newPassword: 'newPass' });
      expect(screen.getByText('profile.passwordSuccess')).toBeInTheDocument();
    });
  });

  it('shows error if passwords do not match', async () => {
    renderComponent('security');

    await waitFor(() => screen.getByRole('heading', { name: 'profile.security' }));

    fireEvent.change(screen.getByLabelText('profile.oldPassword'), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText('profile.password'), { target: { value: 'newPass' } });
    fireEvent.change(screen.getByLabelText('profile.confirmPassword'), { target: { value: 'otherPass' } });

    fireEvent.click(screen.getByRole('button', { name: 'profile.button' }));

    await waitFor(() => {
      expect(screen.getByText('profile.passwordError')).toBeInTheDocument();
      expect(UserManagementService.changeAuthenticatedUserPassword).not.toHaveBeenCalled();
    });
  });

  it('renders SchemaManager when section is schemas', async () => {
    renderComponent('database-schemas');
    await waitFor(() => expect(screen.getByTestId('schema-manager')).toBeInTheDocument());
  });
});
