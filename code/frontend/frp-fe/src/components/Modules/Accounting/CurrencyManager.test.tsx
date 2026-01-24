import { render, screen, waitFor } from '@testing-library/react'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import { CurrencyManager } from './CurrencyManager'
import { AccountingService } from '../../../api/services/AccountingService'
import type { AccCurrencyDto } from '../../../api/models/AccCurrencyDto'
import userEvent from '@testing-library/user-event'

vi.mock('../../../api/services/AccountingService')
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

// Mock flowbite components that might cause issues in JSDOM if complex (Modal sometimes tricky, but usually fine)
// Using real components for now.

describe('CurrencyManager', () => {
  const mockCurrencies: AccCurrencyDto[] = [
    { id: 1, code: 'USD', name: 'US Dollar', scale: 2, isBase: true },
    { id: 2, code: 'EUR', name: 'Euro', scale: 2, isBase: false },
  ]

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(AccountingService.getAllCurrencies).mockResolvedValue(mockCurrencies)
  })

  it('renders list of currencies', async () => {
    render(<CurrencyManager />)

    await waitFor(() => {
        expect(screen.getByText('USD')).toBeDefined()
        expect(screen.getByText('Euro')).toBeDefined()
    })
  })

  it('opens create modal and creates currency', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.createCurrency).mockResolvedValue({ id: 3, code: 'GBP', name: 'Pound', scale: 2, isBase: false })

    render(<CurrencyManager />)

    // Wait for load to finish
    await waitFor(() => expect(screen.getByText('USD')).toBeDefined())

    await user.click(screen.getByText('currency.createTitle'))

    const codeInput = screen.getByLabelText('currency.code')
    const nameInput = screen.getByLabelText('currency.name')
    const scaleInput = screen.getByLabelText('currency.scale')

    await user.type(codeInput, 'GBP')
    await user.type(nameInput, 'Pound')
    
    // Scale might be pre-filled with 2. clear it first?
    await user.clear(scaleInput)
    await user.type(scaleInput, '2')
    
    await user.click(screen.getByText('currency.create'))

    await waitFor(() => {
        expect(AccountingService.createCurrency).toHaveBeenCalledWith({
            code: 'GBP',
            name: 'Pound',
            scale: 2,
            isBase: false
        })
    })
  })

  it('handles delete currency', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(AccountingService.deleteCurrency).mockResolvedValue(undefined)

    render(<CurrencyManager />)

    await waitFor(() => expect(screen.getByText('USD')).toBeDefined())

    // Find delete button for EUR (id: 2)
    const deleteButtons = screen.getAllByText('currency.delete')
    await user.click(deleteButtons[1]) // Index 1 should be EUR

    await waitFor(() => {
        expect(AccountingService.deleteCurrency).toHaveBeenCalledWith(2)
    })
  })

  it('handles set base currency', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.setBaseCurrency).mockResolvedValue(undefined)

    render(<CurrencyManager />)

    await waitFor(() => expect(screen.getByText('USD')).toBeDefined())

    const setBaseButtons = screen.getAllByText('currency.setBase')
    await user.click(setBaseButtons[0]) // Only one button (EUR), USD is base so no button

    await waitFor(() => {
        expect(AccountingService.setBaseCurrency).toHaveBeenCalledWith(2)
    })
  })

  it('displays error on load failure', async () => {
    vi.mocked(AccountingService.getAllCurrencies).mockRejectedValue(new Error('Load failed'))

    render(<CurrencyManager />)

    await waitFor(() => {
        expect(screen.getByText('currency.error')).toBeDefined()
    })
  })
})
