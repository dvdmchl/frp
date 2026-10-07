import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AccountingModule } from './AccountingModule'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

// Mock CurrencyManager to avoid full render
vi.mock('./CurrencyManager', () => ({
  CurrencyManager: () => <div>MockCurrencyManager</div>,
}))

vi.mock('./AccountTree', () => ({
  AccountTree: () => <div>MockAccountTree</div>,
}))

vi.mock('./AccountDetailPage', () => ({
  AccountDetailPage: () => <div>MockAccountDetailPage</div>,
}))

vi.mock('./Connections/ConnectionsPage', () => ({
  ConnectionsPage: () => <div>MockConnectionsPage</div>,
}))

vi.mock('./Connections/ConnectionDetailPage', () => ({
  ConnectionDetailPage: () => <div>MockConnectionDetailPage</div>,
}))

describe('AccountingModule', () => {
  it('renders the account tree at root without links to other pages', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AccountingModule />
      </MemoryRouter>,
    )
    expect(screen.getByText('MockAccountTree')).toBeDefined()
    expect(screen.queryByText('currency.title')).toBeNull()
    expect(screen.queryByText('connection.title')).toBeNull()
  })

  it('renders currency manager at /currencies', () => {
    render(
      <MemoryRouter initialEntries={['/currencies']}>
        <AccountingModule />
      </MemoryRouter>,
    )
    expect(screen.getByText('MockCurrencyManager')).toBeDefined()
  })

  it('renders account detail at /accounts/:accountId', () => {
    render(
      <MemoryRouter initialEntries={['/accounts/42']}>
        <AccountingModule />
      </MemoryRouter>,
    )
    expect(screen.getByText('MockAccountDetailPage')).toBeDefined()
  })

  it('renders connections at /connections', () => {
    render(
      <MemoryRouter initialEntries={['/connections']}>
        <AccountingModule />
      </MemoryRouter>,
    )
    expect(screen.getByText('MockConnectionsPage')).toBeDefined()
  })

  it('renders connection detail at /connections/:connectionId', () => {
    render(
      <MemoryRouter initialEntries={['/connections/7']}>
        <AccountingModule />
      </MemoryRouter>,
    )
    expect(screen.getByText('MockConnectionDetailPage')).toBeDefined()
  })
})
