import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccConnectionDto } from '../../../../api/models/AccConnectionDto'
import { apiError } from '../../../../test/apiError'
import { ConnectionsPage } from './ConnectionsPage'

vi.mock('../../../../api/services/AccountingService')

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const connections: AccConnectionDto[] = [
  { id: 1, name: 'Active wallet', connectorType: 'WALLET', enabled: true, credentialsSet: true },
  { id: 2, name: 'Paused wallet', connectorType: 'WALLET', enabled: false, credentialsSet: true },
  {
    id: 3,
    name: 'Rejected wallet',
    connectorType: 'WALLET',
    enabled: true,
    credentialsSet: true,
    credentialsRejected: true,
  },
  { id: 4, name: 'Empty wallet', connectorType: 'WALLET', enabled: true, credentialsSet: false },
]

const renderPage = () =>
  render(
    <MemoryRouter>
      <ConnectionsPage />
    </MemoryRouter>,
  )

const row = async (name: string) => (await screen.findByText(name)).closest('tr') as HTMLElement

describe('ConnectionsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(AccountingService.getConnections).mockResolvedValue(connections)
    vi.mocked(AccountingService.getConnectors).mockResolvedValue([
      { type: 'WALLET', credentialFields: [{ name: 'apiToken', secret: true }] },
    ])
  })

  it('lists connections with their state', async () => {
    renderPage()

    expect(within(await row('Active wallet')).getByText('connection.state.active')).toBeInTheDocument()
    expect(within(await row('Paused wallet')).getByText('connection.state.disabled')).toBeInTheDocument()
    expect(within(await row('Rejected wallet')).getByText('connection.state.credentialsRejected')).toBeInTheDocument()
    expect(within(await row('Empty wallet')).getByText('connection.state.noCredentials')).toBeInTheDocument()
  })

  it('shows an empty message without connections', async () => {
    vi.mocked(AccountingService.getConnections).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('connection.empty')).toBeInTheDocument()
  })

  it('shows the API error when loading fails', async () => {
    vi.mocked(AccountingService.getConnections).mockRejectedValue(apiError('Not allowed', 403))
    renderPage()

    expect(await screen.findByText('Not allowed')).toBeInTheDocument()
  })

  it('disables an enabled connection', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.setEnabled).mockResolvedValue({})
    renderPage()

    await user.click(within(await row('Active wallet')).getByRole('button', { name: 'connection.disable' }))

    await waitFor(() => expect(AccountingService.setEnabled).toHaveBeenCalledWith(1, { enabled: false }))
    expect(await screen.findByText('connection.disableSuccess')).toBeInTheDocument()
    expect(AccountingService.getConnections).toHaveBeenCalledTimes(2)
  })

  it('enables a disabled connection', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.setEnabled).mockResolvedValue({})
    renderPage()

    await user.click(within(await row('Paused wallet')).getByRole('button', { name: 'connection.enable' }))

    await waitFor(() => expect(AccountingService.setEnabled).toHaveBeenCalledWith(2, { enabled: true }))
    expect(await screen.findByText('connection.enableSuccess')).toBeInTheDocument()
  })

  it('reports a successful connection test', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.testConnection).mockResolvedValue(undefined)
    renderPage()

    await user.click(within(await row('Active wallet')).getByRole('button', { name: 'connection.test' }))

    expect(await screen.findByText('connection.testSuccess')).toBeInTheDocument()
  })

  it('shows the error of a rate limited connection test', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.testConnection).mockRejectedValue(apiError('Try again later', 429))
    renderPage()

    await user.click(within(await row('Active wallet')).getByRole('button', { name: 'connection.test' }))

    expect(await screen.findByText('Try again later')).toBeInTheDocument()
    expect(screen.queryByText('connection.testSuccess')).not.toBeInTheDocument()
  })

  it('starts a synchronization of an enabled connection', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.syncNow).mockResolvedValue(undefined)
    renderPage()

    expect(within(await row('Paused wallet')).getByRole('button', { name: 'connection.syncNow' })).toBeDisabled()
    await user.click(within(await row('Active wallet')).getByRole('button', { name: 'connection.syncNow' }))

    await waitFor(() => expect(AccountingService.syncNow).toHaveBeenCalledWith(1))
    expect(await screen.findByText('connection.syncStarted')).toBeInTheDocument()
  })

  it('deletes a connection after confirmation', async () => {
    const user = userEvent.setup()
    vi.spyOn(globalThis, 'confirm').mockReturnValue(true)
    vi.mocked(AccountingService.deleteConnection).mockResolvedValue(undefined)
    renderPage()

    await user.click(within(await row('Active wallet')).getByRole('button', { name: 'common.delete' }))

    await waitFor(() => expect(AccountingService.deleteConnection).toHaveBeenCalledWith(1))
    expect(await screen.findByText('connection.deleteSuccess')).toBeInTheDocument()
  })

  it('keeps a connection when deletion is not confirmed', async () => {
    const user = userEvent.setup()
    vi.spyOn(globalThis, 'confirm').mockReturnValue(false)
    renderPage()

    await user.click(within(await row('Active wallet')).getByRole('button', { name: 'common.delete' }))

    expect(AccountingService.deleteConnection).not.toHaveBeenCalled()
  })

  it('opens the form for a new connection and reloads after saving', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.createConnection).mockResolvedValue({ id: 8, syncIntervalMinutes: 360 })
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'connection.createTitle' }))
    await user.type(screen.getByLabelText('connection.name'), 'New wallet')
    await user.type(screen.getByLabelText('connection.credentialFields.apiToken'), 'token')
    await user.click(screen.getByRole('button', { name: 'connection.save' }))

    expect(await screen.findByText('connection.saveSuccess')).toBeInTheDocument()
    expect(AccountingService.getConnections).toHaveBeenCalledTimes(2)
  })

  it('opens the form with the edited connection', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(within(await row('Active wallet')).getByRole('button', { name: 'common.edit' }))

    expect(screen.getByText('connection.editTitle')).toBeInTheDocument()
    expect(screen.getByLabelText('connection.name')).toHaveValue('Active wallet')
    await user.click(screen.getByRole('button', { name: 'common.close' }))
    await waitFor(() => expect(screen.queryByText('connection.editTitle')).not.toBeInTheDocument())
  })
})
