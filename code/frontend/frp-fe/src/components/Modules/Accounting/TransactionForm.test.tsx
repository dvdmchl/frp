import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { TransactionForm } from './TransactionForm'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const accounts = [
  { id: 10, name: 'Checking', currencyCode: 'USD' },
  { id: 20, name: 'Income', currencyCode: 'USD' },
]

describe('TransactionForm', () => {
  it('submits a balanced transaction with journal entries', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(<TransactionForm accounts={accounts} selectedAccountId={10} onSubmit={onSubmit} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('transaction.reference'), 'INV-42')
    const accountFields = screen.getAllByLabelText('journal.account')
    const creditFields = screen.getAllByLabelText('journal.credit')
    const debitFields = screen.getAllByLabelText('journal.debit')
    await user.type(creditFields[0], '125.50')
    await user.selectOptions(accountFields[1], '20')
    await user.type(debitFields[1], '125.50')
    await user.click(screen.getByRole('button', { name: 'transaction.create' }))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        reference: 'INV-42',
        journals: [
          expect.objectContaining({ accountId: 10, credit: 125.5, debit: 0 }),
          expect.objectContaining({ accountId: 20, credit: 0, debit: 125.5 }),
        ],
      }),
    )
  })

  it('keeps submission disabled while entries are unbalanced', async () => {
    const user = userEvent.setup()
    render(<TransactionForm accounts={accounts} selectedAccountId={10} onSubmit={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getAllByLabelText('journal.credit')[0], '10')

    expect(screen.getByRole('button', { name: 'transaction.create' })).toBeDisabled()
  })
})
