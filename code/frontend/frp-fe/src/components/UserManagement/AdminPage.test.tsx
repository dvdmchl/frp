import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AdminPage } from './AdminPage'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { Paths } from '../../constants/Paths'

// Mock sub-components
vi.mock('./UserManagementAdmin', () => ({
  UserManagementAdmin: () => <div data-testid="user-management-admin" />,
}))
vi.mock('./MaintenanceAdmin', () => ({
  MaintenanceAdmin: () => <div data-testid="maintenance-admin" />,
}))

// Mock useTranslation
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

describe('AdminPage', () => {
  it('redirects to users section by default', () => {
    render(
      <MemoryRouter initialEntries={[Paths.ADMIN]}>
        <Routes>
          <Route path={Paths.ADMIN} element={<AdminPage />} />
          <Route path={Paths.ADMIN_SECTION} element={<AdminPage />} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByTestId('user-management-admin')).toBeInTheDocument()
  })

  it('renders maintenance section when requested', () => {
    render(
      <MemoryRouter initialEntries={[Paths.ADMIN_MAINTENANCE]}>
        <Routes>
          <Route path={Paths.ADMIN} element={<AdminPage />} />
          <Route path={Paths.ADMIN_SECTION} element={<AdminPage />} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByTestId('maintenance-admin')).toBeInTheDocument()
  })
})
