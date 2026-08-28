import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../api/services/AccountingService'
import { AccountDetailPage } from './AccountDetailPage'

vi.mock('../../../api/services/AccountingService')

const translate = (key: string) => key
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

const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/accounts/10']}>
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
})
