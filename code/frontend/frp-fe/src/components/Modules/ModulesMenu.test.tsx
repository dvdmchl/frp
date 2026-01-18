import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { ModulesMenu } from './ModulesMenu'
import { ModuleManagementService } from '../../api/services/ModuleManagementService'
import { MemoryRouter } from 'react-router-dom'
import type { ModuleDefinitionDto } from '../../api/models/ModuleDefinitionDto'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

// Mock translation
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

describe('ModulesMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders nothing if no enabled modules', async () => {
    vi.spyOn(ModuleManagementService, 'listModules').mockResolvedValue([])
    render(
      <MemoryRouter>
        <ModulesMenu />
      </MemoryRouter>,
    )
    await waitFor(() => {
      expect(screen.queryByText('modules.menuTitle')).not.toBeInTheDocument()
    })
  })

  it('renders dropdown with enabled modules', async () => {
    const mockModules: ModuleDefinitionDto[] = [
      { code: 'mod1', title: 'Module 1', state: 'ENABLED' },
      { code: 'mod2', title: 'Module 2', state: 'DISABLED' },
      { code: 'mod3', title: 'Module 3', state: 'ENABLED' },
    ]
    vi.spyOn(ModuleManagementService, 'listModules').mockResolvedValue(mockModules)

    render(
      <MemoryRouter>
        <ModulesMenu />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('modules.menuTitle')).toBeInTheDocument()
    })

    // Click to open dropdown (Flowbite dropdowns usually require click)
    fireEvent.click(screen.getByText('modules.menuTitle'))

    expect(screen.getByText('Module 1')).toBeInTheDocument()
    expect(screen.queryByText('Module 2')).not.toBeInTheDocument()
    expect(screen.getByText('Module 3')).toBeInTheDocument()
  })

  it('navigates on item click', async () => {
    const mockModules: ModuleDefinitionDto[] = [{ code: 'mod1', title: 'Module 1', state: 'ENABLED' }]
    vi.spyOn(ModuleManagementService, 'listModules').mockResolvedValue(mockModules)

    render(
      <MemoryRouter>
        <ModulesMenu />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('modules.menuTitle')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('modules.menuTitle'))
    fireEvent.click(screen.getByText('Module 1'))

    expect(mockNavigate).toHaveBeenCalledWith('/modules/mod1')
  })
})
