import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { SchemaManager } from './SchemaManager'
import { SchemaManagementService } from '../../api/services/SchemaManagementService'
import { UserManagementService } from '../../api/services/UserManagementService'

// Mock translations
const mockT = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: mockT,
  }),
}))

// Mock Services
vi.mock('../../api/services/SchemaManagementService', () => ({
  SchemaManagementService: {
    listMySchemas: vi.fn(),
    createSchema: vi.fn(),
    copySchema: vi.fn(),
    deleteSchema: vi.fn(),
    setActiveSchema: vi.fn(),
  },
}))

vi.mock('../../api/services/UserManagementService', () => ({
  UserManagementService: {
    authenticatedUser: vi.fn(),
  },
}))

describe('SchemaManager Component', () => {
  const mockUser = { id: 1, email: 'test@example.com', fullName: 'Test User', activeSchema: 'public' }
  const mockSchemas = ['public', 'private']
  const mockOnUserUpdate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    ;(SchemaManagementService.listMySchemas as any).mockResolvedValue(mockSchemas)
    ;(UserManagementService.authenticatedUser as any).mockResolvedValue(mockUser)
    // Mock window.confirm
    vi.spyOn(window, 'confirm').mockImplementation(() => true)
  })

  it('loads and lists schemas', async () => {
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)

    await waitFor(() => {
      expect(screen.getByText('public')).toBeInTheDocument()
      expect(screen.getByText('private')).toBeInTheDocument()
    })
  })

  it('creates new schema', async () => {
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)

    fireEvent.click(screen.getByText('schema.createTitle'))

    await waitFor(() => expect(screen.getByLabelText('schema.name')).toBeInTheDocument())

    fireEvent.change(screen.getByLabelText('schema.name'), { target: { value: 'new_schema' } })
    fireEvent.click(screen.getByRole('button', { name: 'schema.create' }))

    await waitFor(() => {
      expect(SchemaManagementService.createSchema).toHaveBeenCalledWith({ name: 'new_schema' }, false)
      expect(screen.getByText('schema.createSuccess')).toBeInTheDocument()
      expect(SchemaManagementService.listMySchemas).toHaveBeenCalledTimes(2) // Initial + after create
    })
  })

  it('switches active schema', async () => {
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)

    await waitFor(() => screen.getByText('public'))

    // private schema should have switch button
    const rows = screen.getAllByRole('row')
    const privateRow = rows.find((row) => row.textContent?.includes('private'))
    const switchButton = within(privateRow!).getByText('schema.switch')

    fireEvent.click(switchButton)

    await waitFor(() => {
      expect(SchemaManagementService.setActiveSchema).toHaveBeenCalledWith({ name: 'private' })
      expect(UserManagementService.authenticatedUser).toHaveBeenCalled()
      expect(mockOnUserUpdate).toHaveBeenCalled()
    })
  })

  it('deletes schema', async () => {
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    await waitFor(() => screen.getByText('private'))

    const rows = screen.getAllByRole('row')
    const privateRow = rows.find((row) => row.textContent?.includes('private'))
    const deleteButton = within(privateRow!).getByText('schema.delete')

    fireEvent.click(deleteButton)

    await waitFor(() => {
      expect(SchemaManagementService.deleteSchema).toHaveBeenCalledWith('private')
      expect(SchemaManagementService.listMySchemas).toHaveBeenCalledTimes(2)
    })
  })

  // Helper to scope queries
  function within(element: HTMLElement) {
    return {
      getByText: (text: string) => {
        const el = Array.from(element.querySelectorAll('*')).find((e) => e.textContent === text)
        if (!el) throw new Error(`Element with text "${text}" not found`)
        return el
      },
    }
  }
})
