import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AccountForm } from './AccountForm'
import { vi, describe, it, expect } from 'vitest'
import type { AccCurrencyDto } from '../../../api/models/AccCurrencyDto'

// Mock translations
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('flowbite-react', () => ({
  Button: (props: any) => <button {...props} />,
  Checkbox: (props: any) => <input type="checkbox" {...props} />,
  Label: ({ value, children, ...props }: any) => <label {...props}>{value || children}</label>,
  Select: (props: any) => <select {...props} />,
  Textarea: (props: any) => <textarea {...props} />,
}))

const mockCurrencies: AccCurrencyDto[] = [
  { id: 1, code: 'USD', name: 'Dollar', scale: 2 },
  { id: 2, code: 'EUR', name: 'Euro', scale: 2 },
]

const mockParents = [
  { id: 10, name: 'Assets' },
  { id: 20, name: 'Liabilities' },
]

describe('AccountForm', () => {
  it('renders create form correctly', () => {
    render(
      <AccountForm currencies={mockCurrencies} possibleParents={mockParents} onSubmit={vi.fn()} onCancel={vi.fn()} />,
    )

    expect(screen.getByText('account.createTitle')).toBeInTheDocument()
    expect(screen.getByLabelText('account.name')).toBeInTheDocument()
    expect(screen.getByLabelText('account.type')).toBeInTheDocument()
  })

  it('renders edit form correctly', async () => {
    const initialData = { id: 123, name: 'Test Account', currencyCode: 'USD', accountType: 'ASSET' as const }
    render(
      <AccountForm
        initialData={initialData}
        currencies={mockCurrencies}
        possibleParents={mockParents}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    expect(screen.getByText('account.editTitle')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByDisplayValue('Test Account')).toBeInTheDocument()
      expect(screen.getByDisplayValue('USD - Dollar')).toBeInTheDocument()
    })
  })

  it('validates required fields', async () => {
    const onSubmit = vi.fn()
    render(
      <AccountForm currencies={mockCurrencies} possibleParents={mockParents} onSubmit={onSubmit} onCancel={vi.fn()} />,
    )

    fireEvent.click(screen.getByText('account.create'))

    await waitFor(() => {
      expect(screen.getAllByText('Required')).toHaveLength(2) // Name and Currency (since not placeholder)
    })
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits valid data', async () => {
    const onSubmit = vi.fn()
    render(
      <AccountForm currencies={mockCurrencies} possibleParents={mockParents} onSubmit={onSubmit} onCancel={vi.fn()} />,
    )

    fireEvent.change(screen.getByLabelText('account.name'), { target: { value: 'New Account' } })
    fireEvent.change(screen.getByLabelText('account.type'), { target: { value: 'ASSET' } })
    // Select currency
    fireEvent.change(screen.getByLabelText('account.currency'), { target: { value: 'USD' } })

    fireEvent.click(screen.getByText('account.create'))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'New Account',
          accountType: 'ASSET',
          currencyCode: 'USD',
        }),
      )
    })
  })

  it('hides currency and liquid for placeholder', async () => {
    render(
      <AccountForm currencies={mockCurrencies} possibleParents={mockParents} onSubmit={vi.fn()} onCancel={vi.fn()} />,
    )

    const checkbox = screen.getByLabelText('account.isPlaceholder')
    fireEvent.click(checkbox)

    await waitFor(() => {
      expect(screen.queryByLabelText('account.currency')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('account.isLiquid')).not.toBeInTheDocument()
    })
  })
})
