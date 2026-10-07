import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccConnectorDto } from '../../../../api/models/AccConnectorDto'
import { apiError } from '../../../../test/apiError'
import { ConnectionForm } from './ConnectionForm'

vi.mock('../../../../api/services/AccountingService')

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const connectors: AccConnectorDto[] = [
  {
    type: 'WALLET',
    credentialFields: [
      { name: 'apiToken', secret: true },
      { name: 'user', secret: false },
    ],
  },
  { type: 'BANK', credentialFields: [{ name: 'iban', secret: false }] },
]

const existingConnection = {
  id: 5,
  connectorType: 'WALLET',
  name: 'Wallet',
  syncIntervalMinutes: 60,
  syncSettings: { importStartDate: '2026-01-01', other: 'x' },
}

const renderForm = (connection?: typeof existingConnection) => {
  const onSaved = vi.fn()
  const onCancel = vi.fn()
  render(<ConnectionForm connectors={connectors} connection={connection} onSaved={onSaved} onCancel={onCancel} />)
  return { onSaved, onCancel }
}

const fillCredentials = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.type(screen.getByLabelText('connection.credentialFields.apiToken'), 'secret-token')
  await user.type(screen.getByLabelText('connection.credentialFields.user'), 'jane')
}

describe('ConnectionForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders credential fields of the selected connector with secrets masked', () => {
    renderForm()

    expect(screen.getByLabelText('connection.credentialFields.apiToken')).toHaveAttribute('type', 'password')
    expect(screen.getByLabelText('connection.credentialFields.user')).toHaveAttribute('type', 'text')
  })

  it('disables saving until all credentials of a new connection are filled', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.type(screen.getByLabelText('connection.name'), 'My wallet')

    expect(screen.getByRole('button', { name: 'connection.save' })).toBeDisabled()
    await fillCredentials(user)
    expect(screen.getByRole('button', { name: 'connection.save' })).toBeEnabled()
  })

  it('creates a connection with credentials and the import start date', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.createConnection).mockResolvedValue({ id: 9, syncIntervalMinutes: 360 })
    const { onSaved } = renderForm()

    await user.type(screen.getByLabelText('connection.name'), 'My wallet')
    await fillCredentials(user)
    await user.type(screen.getByLabelText('connection.importStartDate'), '2026-03-01')
    await user.click(screen.getByRole('button', { name: 'connection.save' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(AccountingService.createConnection).toHaveBeenCalledWith({
      connectorType: 'WALLET',
      name: 'My wallet',
      credentials: { apiToken: 'secret-token', user: 'jane' },
      syncSettings: { importStartDate: '2026-03-01' },
    })
    expect(AccountingService.updateConnection).not.toHaveBeenCalled()
  })

  it('sets a changed sync interval after creating the connection', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.createConnection).mockResolvedValue({
      id: 9,
      syncIntervalMinutes: 360,
      syncSettings: {},
    })
    vi.mocked(AccountingService.updateConnection).mockResolvedValue({ id: 9 })
    const { onSaved } = renderForm()

    await user.type(screen.getByLabelText('connection.name'), 'My wallet')
    await fillCredentials(user)
    await user.clear(screen.getByLabelText('connection.syncInterval'))
    await user.type(screen.getByLabelText('connection.syncInterval'), '120')
    await user.click(screen.getByRole('button', { name: 'connection.save' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(AccountingService.updateConnection).toHaveBeenCalledWith(9, {
      name: 'My wallet',
      syncSettings: {},
      syncIntervalMinutes: 120,
    })
  })

  it('switches credential fields when another connector is picked', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.selectOptions(screen.getByLabelText('connection.connectorType'), 'BANK')

    expect(screen.getByLabelText('connection.credentialFields.iban')).toBeInTheDocument()
    expect(screen.queryByLabelText('connection.credentialFields.apiToken')).not.toBeInTheDocument()
  })

  it('shows the error when the source rejects the credentials', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.createConnection).mockRejectedValue(apiError('Credentials rejected', 422))
    const { onSaved } = renderForm()

    await user.type(screen.getByLabelText('connection.name'), 'My wallet')
    await fillCredentials(user)
    await user.click(screen.getByRole('button', { name: 'connection.save' }))

    expect(await screen.findByText('Credentials rejected')).toBeInTheDocument()
    expect(onSaved).not.toHaveBeenCalled()
  })

  it('updates a connection and keeps its stored credentials when none are entered', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.updateConnection).mockResolvedValue(existingConnection)
    const { onSaved } = renderForm(existingConnection)

    expect(screen.getByLabelText('connection.connectorType')).toBeDisabled()
    expect(screen.getByText('connection.credentialsHint')).toBeInTheDocument()
    await user.clear(screen.getByLabelText('connection.name'))
    await user.type(screen.getByLabelText('connection.name'), 'Renamed')
    await user.clear(screen.getByLabelText('connection.importStartDate'))
    await user.click(screen.getByRole('button', { name: 'connection.save' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(AccountingService.updateConnection).toHaveBeenCalledWith(5, {
      name: 'Renamed',
      syncSettings: { other: 'x' },
      syncIntervalMinutes: 60,
    })
    expect(AccountingService.setCredentials).not.toHaveBeenCalled()
  })

  it('replaces the credentials of a connection when new ones are entered', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.updateConnection).mockResolvedValue(existingConnection)
    vi.mocked(AccountingService.setCredentials).mockResolvedValue(existingConnection)
    const { onSaved } = renderForm(existingConnection)

    await user.type(screen.getByLabelText('connection.credentialFields.apiToken'), 'new-token')
    await user.click(screen.getByRole('button', { name: 'connection.save' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(AccountingService.setCredentials).toHaveBeenCalledWith(5, { credentials: { apiToken: 'new-token' } })
  })

  it('calls cancel when closed', async () => {
    const user = userEvent.setup()
    const { onCancel } = renderForm()

    await user.click(screen.getByRole('button', { name: 'common.close' }))

    expect(onCancel).toHaveBeenCalled()
  })
})
