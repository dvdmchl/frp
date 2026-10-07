import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiError } from '../../../test/apiError'
import { AccountingService } from '../../../api/services/AccountingService'
import { AccountDetailPage } from './AccountDetailPage'

vi.mock('../../../api/services/AccountingService')

const translate = (key: string, options?: { name?: string | number }) =>
  options?.name === undefined ? key : `${key} ${options.name}`
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const tree = [
  {
    id: 1,
    account: { id: 10, name: 'Checking', currencyCode: 'USD', balance: 750, accountType: 'ASSET' },
  },
  {
    id: 2,
    account: { id: 20, name: 'Income', currencyCode: 'USD', balance: 0, accountType: 'REVENUE' },
  },
]

const transactions = [
  {
    id: 100,
    reference: 'PAY-001',
    description: 'Salary',
    fxRate: 1,
    totalAmount: 1000,
    journals: [
      { id: 1001, date: '2026-08-01', accountId: 10, debit: 1000, credit: 0, description: 'Bank deposit' },
      { id: 1002, date: '2026-08-01', accountId: 20, debit: 0, credit: 1000, description: 'Monthly salary' },
    ],
  },
  {
    id: 200,
    reference: 'OTHER',
    description: 'Other account only',
    totalAmount: 50,
    journals: [{ id: 2001, date: '2026-08-02', accountId: 20, debit: 50, credit: 0 }],
  },
]

const renderPage = (accountId = '10') =>
  render(
    <MemoryRouter initialEntries={[`/accounts/${accountId}`]}>
      <Routes>
        <Route path="accounts/:accountId" element={<AccountDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )

describe('AccountDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(AccountingService.getTree).mockResolvedValue(tree as never)
    vi.mocked(AccountingService.getAllTransactions).mockResolvedValue(transactions)
    vi.mocked(AccountingService.getConnections).mockResolvedValue([{ id: 7, name: 'Wallet' }])
  })

  it('marks imported transactions with the name of their source connection', async () => {
    vi.mocked(AccountingService.getAllTransactions).mockResolvedValue([
      { ...transactions[0], sourceConnectionId: 7, sourceExternalId: 'w-1' },
    ])
    renderPage()

    expect(await screen.findByText('PAY-001')).toBeInTheDocument()
    expect(screen.getByText('transaction.importedFrom Wallet')).toBeInTheDocument()
  })

  it('does not mark transactions entered in FRP as imported', async () => {
    renderPage()

    expect(await screen.findByText('PAY-001')).toBeInTheDocument()
    expect(screen.queryByText(/transaction.importedFrom/)).not.toBeInTheDocument()
  })

  it('shows account summaries and only related transactions', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Checking' })).toBeInTheDocument()
    expect(screen.getByText('PAY-001')).toBeInTheDocument()
    expect(screen.queryByText('OTHER')).not.toBeInTheDocument()
    expect(screen.getByText('750.00 USD')).toBeInTheDocument()
    expect(screen.getAllByText('1000.00 USD')).toHaveLength(2)
  })

  it('filters transactions and expands their journal rows', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('PAY-001')

    await user.type(screen.getByLabelText('transaction.search'), 'missing')
    expect(screen.getByText('transaction.empty')).toBeInTheDocument()
    await user.clear(screen.getByLabelText('transaction.search'))
    await user.click(screen.getByText('PAY-001'))

    expect(screen.getByText('Monthly salary')).toBeInTheDocument()
    expect(screen.getByText('Income')).toBeInTheDocument()
  })

  it('deletes a transaction after confirmation', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(AccountingService.deleteTransaction).mockResolvedValue(undefined)
    renderPage()
    await screen.findByText('PAY-001')

    await user.click(screen.getByRole('button', { name: 'common.delete' }))

    await waitFor(() => expect(AccountingService.deleteTransaction).toHaveBeenCalledWith(100))
  })

  it('shows not found message when the account does not exist', async () => {
    renderPage('999')

    expect(await screen.findByText('account.notFound')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'account.backToTree' })).toBeInTheDocument()
  })

  it('shows the API error message when loading fails', async () => {
    vi.mocked(AccountingService.getTree).mockRejectedValue(apiError('Tree unavailable'))
    renderPage()

    expect(await screen.findByText('Tree unavailable')).toBeInTheDocument()
  })

  it('shows a generic error when loading fails without an API error', async () => {
    vi.mocked(AccountingService.getAllTransactions).mockRejectedValue(new Error('network'))
    renderPage()

    expect(await screen.findByText('transaction.error')).toBeInTheDocument()
  })

  it('filters transactions by date range', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('PAY-001')

    await user.type(screen.getByLabelText('transaction.dateFrom'), '2026-08-02')

    expect(screen.getByText('transaction.empty')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('transaction.dateFrom'))
    await user.type(screen.getByLabelText('transaction.dateTo'), '2026-08-01')

    expect(screen.getByText('PAY-001')).toBeInTheDocument()
  })

  it('does not delete a transaction when confirmation is cancelled', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderPage()
    await screen.findByText('PAY-001')

    await user.click(screen.getByRole('button', { name: 'common.delete' }))

    expect(AccountingService.deleteTransaction).not.toHaveBeenCalled()
  })

  it('shows the API error when deleting a transaction fails', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(AccountingService.deleteTransaction).mockRejectedValue(apiError('Delete refused'))
    renderPage()
    await screen.findByText('PAY-001')

    await user.click(screen.getByRole('button', { name: 'common.delete' }))

    expect(await screen.findByText('Delete refused')).toBeInTheDocument()
  })

  it('updates a journal entry from the edit dialog', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.updateJournal).mockResolvedValue({} as never)
    renderPage()
    await user.click(await screen.findByText('PAY-001'))

    await user.click(screen.getAllByRole('button', { name: 'common.edit' })[1])
    const description = await screen.findByLabelText('journal.description')
    await user.clear(description)
    await user.type(description, 'Corrected deposit')
    await user.click(screen.getByRole('button', { name: 'journal.update' }))

    await waitFor(() =>
      expect(AccountingService.updateJournal).toHaveBeenCalledWith(1001, {
        date: '2026-08-01',
        description: 'Corrected deposit',
      }),
    )
  })

  it('deletes a journal entry after confirmation', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(AccountingService.deleteJournal).mockResolvedValue(undefined as never)
    renderPage()
    await user.click(await screen.findByText('PAY-001'))

    await user.click(screen.getAllByRole('button', { name: 'common.delete' })[2])

    await waitFor(() => expect(AccountingService.deleteJournal).toHaveBeenCalledWith(1002))
  })
})
