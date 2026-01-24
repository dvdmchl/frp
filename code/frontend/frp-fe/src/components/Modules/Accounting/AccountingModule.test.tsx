import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AccountingModule } from './AccountingModule'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

// Mock CurrencyManager to avoid full render
vi.mock('./CurrencyManager', () => ({
  CurrencyManager: () => <div>MockCurrencyManager</div>
}))

describe('AccountingModule', () => {
  it('renders dashboard at root', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AccountingModule />
      </MemoryRouter>
    )
    expect(screen.getByText('Accounting Module')).toBeDefined()
  })

  it('renders currency manager at /currencies', () => {
    render(
      <MemoryRouter initialEntries={['/currencies']}>
        <AccountingModule />
      </MemoryRouter>
    )
    expect(screen.getByText('MockCurrencyManager')).toBeDefined()
  })
})
