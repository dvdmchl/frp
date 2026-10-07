import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../../api/services/AccountingService'
import { apiError } from '../../../../test/apiError'
import { ConnectionDetailPage } from './ConnectionDetailPage'

vi.mock('../../../../api/services/AccountingService')

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/connections/7']}>
      <Routes>
        <Route path="connections/:connectionId" element={<ConnectionDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )

describe('ConnectionDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(AccountingService.getConnection).mockResolvedValue({ id: 7, name: 'My wallet', enabled: true })
    vi.mocked(AccountingService.getRecords).mockResolvedValue([])
    vi.mocked(AccountingService.getRuns).mockResolvedValue([])
    vi.mocked(AccountingService.getMappings).mockResolvedValue([])
    vi.mocked(AccountingService.getTree).mockResolvedValue([])
  })

  it('shows the connection with records, mapping and sync history', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: 'My wallet' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'connection.tabs.records' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'connection.tabs.mappings' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'connection.tabs.runs' })).toBeInTheDocument()
    expect(AccountingService.getConnection).toHaveBeenCalledWith(7)
    await waitFor(() => expect(AccountingService.getRecords).toHaveBeenCalled())
  })

  it('starts a synchronization and reloads records and runs', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.syncNow).mockResolvedValue(undefined)
    renderPage()
    await screen.findByRole('heading', { name: 'My wallet' })
    await waitFor(() => expect(AccountingService.getRecords).toHaveBeenCalledTimes(1))

    await user.click(screen.getByRole('button', { name: 'connection.syncNow' }))

    expect(await screen.findByText('connection.syncStarted')).toBeInTheDocument()
    expect(AccountingService.syncNow).toHaveBeenCalledWith(7)
    await waitFor(() => expect(AccountingService.getRecords).toHaveBeenCalledTimes(2))
  })

  it('shows the error when the synchronization cannot start', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.syncNow).mockRejectedValue(apiError('Connection is disabled', 409))
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'connection.syncNow' }))

    expect(await screen.findByText('Connection is disabled')).toBeInTheDocument()
    expect(screen.queryByText('connection.syncStarted')).not.toBeInTheDocument()
  })

  it('disables synchronization of a disabled connection', async () => {
    vi.mocked(AccountingService.getConnection).mockResolvedValue({ id: 7, name: 'My wallet', enabled: false })
    renderPage()

    expect(await screen.findByRole('button', { name: 'connection.syncNow' })).toBeDisabled()
  })

  it('shows the error and a link back when the connection cannot be loaded', async () => {
    vi.mocked(AccountingService.getConnection).mockRejectedValue(apiError('Connection not found', 400))
    renderPage()

    expect(await screen.findByText('Connection not found')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← connection.backToList' })).toBeInTheDocument()
  })
})
