import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MaintenanceAdmin } from './MaintenanceAdmin'
import { MaintenanceAdminService } from '../../api'
import { MemoryRouter } from 'react-router-dom'

// Mock the API service
vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api')>()
  return {
    ...actual,
    MaintenanceAdminService: {
      listOrphanSchemas: vi.fn(),
      dropOrphanSchemas: vi.fn(),
    },
  }
})

// Mock useTranslation
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const mockOrphanSchemas = ['orphan_1', 'orphan_2']

describe('MaintenanceAdmin', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(MaintenanceAdminService.listOrphanSchemas).mockResolvedValue(mockOrphanSchemas)
  })

  it('renders orphan schemas list', async () => {
    render(
      <MemoryRouter>
        <MaintenanceAdmin />
      </MemoryRouter>,
    )

    expect(screen.getByText('admin.maintenance.title')).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByText('orphan_1')).toBeInTheDocument()
      expect(screen.getByText('orphan_2')).toBeInTheDocument()
    })
  })

  it('drops selected schemas', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(MaintenanceAdminService.dropOrphanSchemas).mockResolvedValue(undefined)

    render(
      <MemoryRouter>
        <MaintenanceAdmin />
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('orphan_1')).toBeInTheDocument())

    const checkboxes = screen.getAllByRole('checkbox')
    fireEvent.click(checkboxes[1]) // First orphan schema (index 0 is header checkbox)

    const dropButton = screen.getByText('admin.maintenance.dropSelected')
    fireEvent.click(dropButton)

    await waitFor(() => {
      expect(MaintenanceAdminService.dropOrphanSchemas).toHaveBeenCalledWith(['orphan_1'])
    })
  })
})
