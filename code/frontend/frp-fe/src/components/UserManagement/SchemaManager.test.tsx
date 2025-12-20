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

  it('handles load error', async () => {
    ;(SchemaManagementService.listMySchemas as any).mockRejectedValue(new Error('Load failed'))
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)

    await waitFor(() => {
      expect(screen.getByText('schema.error')).toBeInTheDocument()
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

  it('handles create error', async () => {
    ;(SchemaManagementService.createSchema as any).mockRejectedValue(new Error('Create failed'))
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)

    fireEvent.click(screen.getByText('schema.createTitle'))
    fireEvent.change(screen.getByLabelText('schema.name'), { target: { value: 'new_schema' } })
    fireEvent.click(screen.getByRole('button', { name: 'schema.create' }))

    await waitFor(() => {
      expect(screen.getByText('schema.error')).toBeInTheDocument()
    })
  })

  it('copies schema', async () => {
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    await waitFor(() => screen.getByText('public'))

    const rows = screen.getAllByRole('row')
    const publicRow = rows.find((row) => row.textContent?.includes('public'))
    const copyButton = within(publicRow!).getByText('schema.copy')

    fireEvent.click(copyButton)

    await waitFor(() => expect(screen.getByLabelText('schema.target')).toBeInTheDocument())

    fireEvent.change(screen.getByLabelText('schema.target'), { target: { value: 'target_schema' } })
    fireEvent.click(screen.getByRole('button', { name: 'schema.copy' }))

    await waitFor(() => {
      expect(SchemaManagementService.copySchema).toHaveBeenCalledWith({
        source: 'public',
        target: 'target_schema',
      })
      expect(screen.getByText('schema.copySuccess')).toBeInTheDocument()
    })
  })

  it('handles copy error', async () => {
    ;(SchemaManagementService.copySchema as any).mockRejectedValue(new Error('Copy failed'))
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    await waitFor(() => screen.getByText('public'))

    fireEvent.click(screen.getAllByText('schema.copy')[0])
    fireEvent.change(screen.getByLabelText('schema.target'), { target: { value: 'target' } })
    fireEvent.click(screen.getByRole('button', { name: 'schema.copy' }))

    await waitFor(() => {
      expect(screen.getByText('schema.error')).toBeInTheDocument()
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

  it('handles switch error', async () => {
    ;(SchemaManagementService.setActiveSchema as any).mockRejectedValue(new Error('Switch failed'))
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    await waitFor(() => screen.getByText('private'))

    const rows = screen.getAllByRole('row')
    const privateRow = rows.find((row) => row.textContent?.includes('private'))
    fireEvent.click(within(privateRow!).getByText('schema.switch'))

    await waitFor(() => {
      expect(screen.getByText('schema.error')).toBeInTheDocument()
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

  it('handles delete error', async () => {
    ;(SchemaManagementService.deleteSchema as any).mockRejectedValue(new Error('Delete failed'))
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    await waitFor(() => screen.getByText('private'))

    const rows = screen.getAllByRole('row')
    const privateRow = rows.find((row) => row.textContent?.includes('private'))
    fireEvent.click(within(privateRow!).getByText('schema.delete'))

    await waitFor(() => {
      expect(screen.getByText('schema.error')).toBeInTheDocument()
    })
  })

  it('does not delete when confirm is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockImplementation(() => false)
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    await waitFor(() => screen.getByText('private'))

    fireEvent.click(screen.getAllByText('schema.delete')[0])

    expect(SchemaManagementService.deleteSchema).not.toHaveBeenCalled()
  })

  it('can cancel create modal', async () => {
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    fireEvent.click(screen.getByText('schema.createTitle'))
    await waitFor(() => expect(screen.getByText('Cancel')).toBeInTheDocument())
    fireEvent.click(screen.getByText('Cancel'))
    await waitFor(() => expect(screen.queryByText('Cancel')).not.toBeInTheDocument())
  })

  it('can cancel copy modal', async () => {
    render(<SchemaManager user={mockUser} onUserUpdate={mockOnUserUpdate} />)
    await waitFor(() => screen.getByText('public'))
    fireEvent.click(screen.getAllByText('schema.copy')[0])
    await waitFor(() => expect(screen.getByText('Cancel')).toBeInTheDocument())
    fireEvent.click(screen.getByText('Cancel'))
    await waitFor(() => expect(screen.queryByText('Cancel')).not.toBeInTheDocument())
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
