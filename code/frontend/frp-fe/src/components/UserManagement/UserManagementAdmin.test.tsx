import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { UserManagementAdmin } from './UserManagementAdmin'
import { UserManagementAdminService } from '../../api'
import { MemoryRouter } from 'react-router-dom'

// Mock the API service
vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api')>()
  return {
    ...actual,
    UserManagementAdminService: {
      listUsers: vi.fn(),
      updateActiveStatus: vi.fn(),
      updateAdminStatus: vi.fn(),
    },
  }
})

// Mock useTranslation
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const mockUsers = [
  { id: 1, email: 'admin@test.com', fullName: 'Admin User', active: true, admin: true },
  { id: 2, email: 'user@test.com', fullName: 'Regular User', active: true, admin: false },
]

describe('UserManagementAdmin', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(UserManagementAdminService.listUsers).mockResolvedValue(mockUsers)
  })

  it('renders user list', async () => {
    render(
      <MemoryRouter>
        <UserManagementAdmin />
      </MemoryRouter>,
    )

    expect(screen.getByText('admin.userManagement.title')).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByText('Admin User')).toBeInTheDocument()
      expect(screen.getByText('Regular User')).toBeInTheDocument()
    })
  })

  it('filters users on search', async () => {
    render(
      <MemoryRouter>
        <UserManagementAdmin />
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('Admin User')).toBeInTheDocument())

    const searchInput = screen.getByPlaceholderText('admin.userManagement.searchPlaceholder')
    fireEvent.change(searchInput, { target: { value: 'Admin' } })

    const searchButton = screen.getByText('common.search')
    fireEvent.click(searchButton)

    expect(UserManagementAdminService.listUsers).toHaveBeenCalledWith('Admin')
  })

  it('toggles user active status', async () => {
    const updatedUser = { ...mockUsers[1], active: false }
    vi.mocked(UserManagementAdminService.updateActiveStatus).mockResolvedValue(updatedUser)

    render(
      <MemoryRouter>
        <UserManagementAdmin />
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('Regular User')).toBeInTheDocument())

    const regularUserRow = screen.getByText('Regular User').closest('tr')
    if (!regularUserRow) throw new Error('Regular user row not found')

    const deactivateButton = within(regularUserRow).getByText('admin.userManagement.deactivate')
    fireEvent.click(deactivateButton)

    await waitFor(() => {
      expect(UserManagementAdminService.updateActiveStatus).toHaveBeenCalledWith(2, false)
    })
  })

  it('shows the error when loading users fails', async () => {
    vi.mocked(UserManagementAdminService.listUsers).mockRejectedValue({ message: 'Forbidden' })

    render(
      <MemoryRouter>
        <UserManagementAdmin />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Forbidden')).toBeInTheDocument()
  })

  it('resets the search and reloads all users', async () => {
    render(
      <MemoryRouter>
        <UserManagementAdmin />
      </MemoryRouter>,
    )
    await waitFor(() => expect(screen.getByText('Admin User')).toBeInTheDocument())
    fireEvent.change(screen.getByPlaceholderText('admin.userManagement.searchPlaceholder'), {
      target: { value: 'Admin' },
    })

    fireEvent.click(screen.getByText('common.reset'))

    expect(screen.getByPlaceholderText('admin.userManagement.searchPlaceholder')).toHaveValue('')
    expect(UserManagementAdminService.listUsers).toHaveBeenLastCalledWith(undefined)
  })

  it('toggles admin privileges from the details dialog', async () => {
    const updatedUser = { ...mockUsers[1], admin: true }
    vi.mocked(UserManagementAdminService.updateAdminStatus).mockResolvedValue(updatedUser)

    render(
      <MemoryRouter>
        <UserManagementAdmin />
      </MemoryRouter>,
    )
    await waitFor(() => expect(screen.getByText('Regular User')).toBeInTheDocument())
    const regularUserRow = screen.getByText('Regular User').closest('tr')
    if (!regularUserRow) throw new Error('Regular user row not found')

    fireEvent.click(within(regularUserRow).getByText('common.details'))
    expect(await screen.findByText('common.none')).toBeInTheDocument()
    fireEvent.click(screen.getAllByRole('switch')[1])

    await waitFor(() => {
      expect(UserManagementAdminService.updateAdminStatus).toHaveBeenCalledWith(2, true)
    })
  })

  it('shows the error when changing admin privileges fails', async () => {
    vi.mocked(UserManagementAdminService.updateAdminStatus).mockRejectedValue({ message: 'Not allowed' })

    render(
      <MemoryRouter>
        <UserManagementAdmin />
      </MemoryRouter>,
    )
    await waitFor(() => expect(screen.getByText('Regular User')).toBeInTheDocument())
    const regularUserRow = screen.getByText('Regular User').closest('tr')
    if (!regularUserRow) throw new Error('Regular user row not found')

    fireEvent.click(within(regularUserRow).getByText('common.details'))
    fireEvent.click((await screen.findAllByRole('switch'))[1])

    expect(await screen.findByText('Not allowed')).toBeInTheDocument()
  })
})
