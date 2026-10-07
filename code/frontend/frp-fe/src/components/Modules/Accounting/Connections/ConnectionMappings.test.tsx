import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccExternalMappingDto } from '../../../../api/models/AccExternalMappingDto'
import type { AccNodeDto } from '../../../../api/models/AccNodeDto'
import { apiError } from '../../../../test/apiError'
import { ConnectionMappings } from './ConnectionMappings'

vi.mock('../../../../api/services/AccountingService')

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const tree: AccNodeDto[] = [
  {
    id: 100,
    isPlaceholder: true,
    account: { id: 1, name: 'Assets', accountType: 'ASSET' },
    children: [
      { id: 101, account: { id: 2, name: 'Cash', accountType: 'ASSET' } },
      { id: 102, account: { id: 3, name: 'Credit card', accountType: 'LIABILITY' } },
    ],
  },
  { id: 200, account: { id: 4, name: 'Groceries', accountType: 'EXPENSE' } },
  { id: 300, account: { id: 5, name: 'Salary', accountType: 'REVENUE' } },
]

const mappings: AccExternalMappingDto[] = [
  { id: 11, kind: 'ACCOUNT', externalId: 'a1', externalName: 'Wallet cash', currencyCode: 'CZK', accountId: 2 },
  { id: 12, kind: 'ACCOUNT', externalId: 'a2', externalName: 'Wallet card', currencyCode: 'CZK' },
  { id: 21, kind: 'CATEGORY', externalId: 'c1', externalName: 'Food', ignored: true },
]

const connection = { id: 7, name: 'Wallet', fallbackExpenseAccountId: 4 }

const renderMappings = () => {
  const onConnectionChange = vi.fn()
  render(<ConnectionMappings connection={connection} onConnectionChange={onConnectionChange} />)
  return { onConnectionChange }
}

const optionNames = (select: HTMLElement) =>
  within(select)
    .getAllByRole('option')
    .map((option) => option.textContent)

describe('ConnectionMappings', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(AccountingService.getMappings).mockResolvedValue(mappings)
    vi.mocked(AccountingService.getTree).mockResolvedValue(tree)
  })

  it('offers only postable accounts of the matching types', async () => {
    renderMappings()

    const accountSelect = await screen.findByLabelText('mapping.account Wallet cash')
    expect(accountSelect).toHaveValue('2')
    expect(optionNames(accountSelect)).toEqual(['common.none', 'Cash', 'Credit card'])
    expect(screen.getByText('Food')).toBeInTheDocument()
    expect(optionNames(screen.getByLabelText('mapping.fallbackExpense'))).toEqual(['common.none', 'Groceries'])
    expect(optionNames(screen.getByLabelText('mapping.fallbackRevenue'))).toEqual(['common.none', 'Salary'])
  })

  it('maps an external account to an account', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.updateMapping).mockResolvedValue({ ...mappings[1], accountId: 3 })
    renderMappings()
    await screen.findByText('Wallet card')

    await user.selectOptions(screen.getByLabelText('mapping.account Wallet card'), 'Credit card')

    await waitFor(() =>
      expect(AccountingService.updateMapping).toHaveBeenCalledWith(7, 12, { accountId: 3, ignored: false }),
    )
    expect(await screen.findAllByDisplayValue('Credit card')).toHaveLength(1)
  })

  it('stops ignoring an external category', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.updateMapping).mockResolvedValue({ ...mappings[2], ignored: false })
    renderMappings()

    await user.click(await screen.findByLabelText('mapping.ignored Food'))

    await waitFor(() =>
      expect(AccountingService.updateMapping).toHaveBeenCalledWith(7, 21, { accountId: undefined, ignored: false }),
    )
  })

  it('creates missing accounts under the chosen node and reloads the mappings', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.createMissingAccounts).mockResolvedValue([])
    renderMappings()
    await screen.findByText('Wallet card')

    await user.selectOptions(screen.getByLabelText('mapping.parentNode', { selector: '#parent-ACCOUNT' }), 'Assets')
    const [createAccounts, createCategories] = screen.getAllByRole('button', { name: 'mapping.createMissing' })
    expect(createCategories).toBeDisabled()
    await user.click(createAccounts)

    await waitFor(() =>
      expect(AccountingService.createMissingAccounts).toHaveBeenCalledWith(7, { kind: 'ACCOUNT', parentNodeId: 100 }),
    )
    await waitFor(() => expect(AccountingService.getMappings).toHaveBeenCalledTimes(2))
  })

  it('shows the error when creating missing accounts fails', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.createMissingAccounts).mockRejectedValue(apiError('Currency CZK is missing', 422))
    renderMappings()
    await screen.findByText('Wallet card')

    await user.click(screen.getAllByRole('button', { name: 'mapping.createMissing' })[0])

    expect(await screen.findByText('Currency CZK is missing')).toBeInTheDocument()
    expect(AccountingService.createMissingAccounts).toHaveBeenCalledWith(7, {
      kind: 'ACCOUNT',
      parentNodeId: undefined,
    })
    expect(AccountingService.getMappings).toHaveBeenCalledTimes(1)
  })

  it('refreshes the mappings from the source', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.refreshMappings).mockResolvedValue([
      ...mappings,
      { id: 13, kind: 'ACCOUNT', externalId: 'a3', externalName: 'Savings' },
    ])
    renderMappings()
    await screen.findByText('Wallet card')

    await user.click(screen.getByRole('button', { name: 'mapping.refresh' }))

    expect(await screen.findByText('Savings')).toBeInTheDocument()
  })

  it('saves the fallback accounts', async () => {
    const user = userEvent.setup()
    const updated = { ...connection, fallbackRevenueAccountId: 5 }
    vi.mocked(AccountingService.setFallbackAccounts).mockResolvedValue(updated)
    const { onConnectionChange } = renderMappings()
    await screen.findByText('Wallet card')

    await user.selectOptions(screen.getByLabelText('mapping.fallbackRevenue'), 'Salary')
    await user.click(screen.getByRole('button', { name: 'mapping.saveFallback' }))

    await waitFor(() =>
      expect(AccountingService.setFallbackAccounts).toHaveBeenCalledWith(7, {
        expenseAccountId: 4,
        revenueAccountId: 5,
      }),
    )
    expect(onConnectionChange).toHaveBeenCalledWith(updated)
  })

  it('shows the API error when loading fails', async () => {
    vi.mocked(AccountingService.getMappings).mockRejectedValue(apiError('Connection not found', 400))
    renderMappings()

    expect(await screen.findByText('Connection not found')).toBeInTheDocument()
    expect(screen.getAllByText('mapping.empty')).toHaveLength(2)
  })
})
