import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccImportRecordDetailDto } from '../../../../api/models/AccImportRecordDetailDto'
import { apiError } from '../../../../test/apiError'
import { ImportRecordDetail } from './ImportRecordDetail'

vi.mock('../../../../api/services/AccountingService')

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const detail: AccImportRecordDetailDto = {
  importRecord: {
    id: 5,
    externalId: 'r1',
    recordDate: '2026-09-01',
    amount: 0,
    currencyCode: 'CZK',
    counterparty: 'Bistro',
    note: 'Lunch',
    sourceState: 'BOOKED',
    status: 'ERROR',
    errorMessage: 'Record Cash → Groceries has a zero amount',
    lastSeenAt: '2026-09-02T10:00:00Z',
  },
  account: { externalId: 'acc-cash', externalName: 'Cash', accountName: 'Wallet cash' },
  category: { externalId: 'cat-food', externalName: 'Food' },
  baseAmount: 0,
  baseCurrencyCode: 'EUR',
  firstSeenAt: '2026-09-01T10:00:00Z',
  rawPayload: '{"note":"Lunch","amount":{"value":0}}',
}

const onClose = vi.fn()

const renderDetail = () => render(<ImportRecordDetail connectionId={7} recordId={5} onClose={onClose} />)

const valueOf = (labelKey: string) => screen.getByText(labelKey).nextElementSibling?.textContent

describe('ImportRecordDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(AccountingService.getRecordDetail).mockResolvedValue(detail)
  })

  it('shows the record with its mappings', async () => {
    renderDetail()

    expect(await screen.findByText('Record Cash → Groceries has a zero amount')).toBeInTheDocument()
    expect(AccountingService.getRecordDetail).toHaveBeenCalledWith(7, 5)
    expect(valueOf('importRecord.amount')).toBe('0.00 CZK')
    expect(valueOf('importRecord.detail.baseAmount')).toBe('0.00 EUR')
    expect(valueOf('importRecord.detail.counterparty')).toBe('Bistro')
    expect(valueOf('importRecord.detail.note')).toBe('Lunch')
    expect(valueOf('importRecord.detail.account')).toBe('Cash → Wallet cash')
    expect(valueOf('importRecord.detail.category')).toBe('Food → importRecord.detail.notMapped')
    expect(valueOf('importRecord.status')).toBe('importRecord.statuses.ERROR')
    expect(screen.queryByText('importRecord.detail.sourceUpdatedAt')).not.toBeInTheDocument()
  })

  it('pretty-prints the raw data from the source', async () => {
    renderDetail()

    const payload = await screen.findByText(/"amount": \{/)
    expect(payload.textContent).toContain('"note": "Lunch"')
  })

  it('keeps raw data that is not JSON as it is', async () => {
    vi.mocked(AccountingService.getRecordDetail).mockResolvedValue({ ...detail, rawPayload: 'not json' })
    renderDetail()

    expect(await screen.findByText('not json')).toBeInTheDocument()
  })

  it('shows the account of the other leg of a transfer instead of the category', async () => {
    vi.mocked(AccountingService.getRecordDetail).mockResolvedValue({
      ...detail,
      importRecord: { ...detail.importRecord, transferLinkId: 't-1' },
      category: undefined,
      transferAccount: { externalId: 'acc-bank', externalName: 'Bank', accountName: 'Bank account' },
    })
    renderDetail()

    await screen.findByText('importRecord.detail.transferAccount')
    expect(valueOf('importRecord.detail.transferAccount')).toBe('Bank → Bank account')
    expect(screen.queryByText('importRecord.detail.category')).not.toBeInTheDocument()
  })

  it('names a missing category and a transfer leg that is not imported yet', async () => {
    vi.mocked(AccountingService.getRecordDetail).mockResolvedValue({ ...detail, category: undefined })
    const { unmount } = renderDetail()
    await screen.findByText('importRecord.detail.category')
    expect(valueOf('importRecord.detail.category')).toBe('importRecord.detail.uncategorized')
    unmount()

    vi.mocked(AccountingService.getRecordDetail).mockResolvedValue({
      ...detail,
      importRecord: { ...detail.importRecord, transferLinkId: 't-1' },
    })
    renderDetail()
    await screen.findByText('importRecord.detail.transferAccount')
    expect(valueOf('importRecord.detail.transferAccount')).toBe('importRecord.detail.otherLegMissing')
  })

  it('shows only the raw data of a detail without the record', async () => {
    vi.mocked(AccountingService.getRecordDetail).mockResolvedValue({ rawPayload: 'raw' })
    renderDetail()

    expect(await screen.findByText('raw')).toBeInTheDocument()
    expect(screen.queryByText('importRecord.date')).not.toBeInTheDocument()
  })

  it('shows the error when the detail cannot be loaded', async () => {
    vi.mocked(AccountingService.getRecordDetail).mockRejectedValue(apiError('Import record not found: 5'))
    renderDetail()

    expect(await screen.findByText('Import record not found: 5')).toBeInTheDocument()
  })

  it('closes', async () => {
    const user = userEvent.setup()
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'common.close' }))

    expect(onClose).toHaveBeenCalled()
  })
})
