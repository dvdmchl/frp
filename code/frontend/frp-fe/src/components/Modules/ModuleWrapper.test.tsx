import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { ModuleWrapper } from './ModuleWrapper'
import { ModuleManagementService } from '../../api/services/ModuleManagementService'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import type { ModuleDefinitionDto } from '../../api/models/ModuleDefinitionDto'
import { CancelablePromise } from '../../api/core/CancelablePromise'

// Mock translation
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, options?: any) => {
      if (key === 'module.disabled.message' && options?.module) {
        return `Module ${options.module} is disabled`
      }
      return key
    },
  }),
}))

describe('ModuleWrapper', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const renderComponent = (moduleCode: string) => {
    return render(
      <MemoryRouter initialEntries={[`/modules/${moduleCode}`]}>
        <Routes>
          <Route path="/modules/:moduleCode" element={<ModuleWrapper />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  it('renders loading state initially', () => {
    vi.spyOn(ModuleManagementService, 'getModule').mockReturnValue(new CancelablePromise(() => {}))
    renderComponent('test-module')
    // Flowbite Spinner usually has role="status" or we can look for generic spinner class/element
    // But since I used <Spinner>, I'll check if it exists.
    // Flowbite Spinner renders an SVG.
    expect(document.querySelector('span[role="status"]')).toBeInTheDocument()
  })

  it('renders module content when enabled', async () => {
    const mockModule: ModuleDefinitionDto = {
      code: 'test-module',
      title: 'Test Module',
      description: 'A test module',
      state: 'ENABLED',
    }
    vi.spyOn(ModuleManagementService, 'getModule').mockResolvedValue(mockModule)

    renderComponent('test-module')

    await waitFor(() => {
      expect(screen.getByText('Test Module')).toBeInTheDocument()
      expect(screen.getByText('A test module')).toBeInTheDocument()
      expect(screen.getByText('module.placeholderContent')).toBeInTheDocument()
    })
  })

  it('renders disabled message when module is disabled', async () => {
    const mockModule: ModuleDefinitionDto = {
      code: 'test-module',
      title: 'Test Module',
      description: 'A test module',
      state: 'DISABLED',
    }
    vi.spyOn(ModuleManagementService, 'getModule').mockResolvedValue(mockModule)

    renderComponent('test-module')

    await waitFor(() => {
      expect(screen.getByText('module.disabled.title')).toBeInTheDocument()
      expect(screen.getByText('Module Test Module is disabled')).toBeInTheDocument()
    })
  })

  it('renders error when API fails', async () => {
    const error = {
      body: {
        message: 'Network Error',
      },
      status: 500,
    }
    vi.spyOn(ModuleManagementService, 'getModule').mockRejectedValue(error)

    renderComponent('test-module')

    await waitFor(() => {
      expect(screen.getByText('Network Error')).toBeInTheDocument()
    })
  })
})
