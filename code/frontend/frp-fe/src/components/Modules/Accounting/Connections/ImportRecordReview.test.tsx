import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccImportRecordDto } from '../../../../api/models/AccImportRecordDto'
import { apiError } from '../../../../test/apiError'
import { ImportRecordReview } from './ImportRecordReview'

vi.mock('../../../../api/services/AccountingService')

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const records: AccImportRecordDto[] = [
  {
    id: 1,
    recordDate: '2026-09-01',
    amount: -120.5,
    currencyCode: 'CZK',
    counterparty: 'Bistro',
    note: 'Lunch',
    status: 'ERROR',
    errorMessage: 'Currency USD is missing',
  },
  { id: 2, recordDate: '2026-09-02', amount: 50, currencyCode: 'CZK', status: 'CONFLICT' },
  { id: 3, recordDate: '2026-09-03', amount: 10, currencyCode: 'CZK', note: 'Unmapped', status: 'NEW' },
]

const renderReview = () => render(<ImportRecordReview connectionId={7} refreshKey={0} />)

const row = async (text: string) => (await screen.findByText(text)).closest('tr') as HTMLElement

describe('ImportRecordReview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(AccountingService.getRecords).mockResolvedValue(records)
  })

  it('lists records that need attention with their reason', async () => {
    renderReview()

    const errorRow = await row('Bistro - Lunch')
    expect(within(errorRow).getByText('-120.50 CZK')).toBeInTheDocument()
    expect(within(errorRow).getByText('Currency USD is missing')).toBeInTheDocument()
    expect(within(await row('2026-09-02')).getByText('importRecord.reasons.CONFLICT')).toBeInTheDocument()
    expect(within(await row('Unmapped')).getByText('importRecord.reasons.NEW')).toBeInTheDocument()
    expect(AccountingService.getRecords).toHaveBeenCalledWith(7, ['ERROR', 'CONFLICT', 'NEW'])
  })

  it('shows an empty message when nothing needs attention', async () => {
    vi.mocked(AccountingService.getRecords).mockResolvedValue([])
    renderReview()

    expect(await screen.findByText('importRecord.empty')).toBeInTheDocument()
  })

  it('retries a failed record and reloads the list', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.retryRecord).mockResolvedValue({})
    renderReview()

    await user.click(within(await row('Bistro - Lunch')).getByRole('button', { name: 'importRecord.retry' }))

    await waitFor(() => expect(AccountingService.retryRecord).toHaveBeenCalledWith(7, 1))
    await waitFor(() => expect(AccountingService.getRecords).toHaveBeenCalledTimes(2))
  })

  it('ignores a record waiting for a mapping', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.ignoreRecord).mockResolvedValue({})
    renderReview()

    await user.click(within(await row('Unmapped')).getByRole('button', { name: 'importRecord.ignore' }))

    await waitFor(() => expect(AccountingService.ignoreRecord).toHaveBeenCalledWith(7, 3))
  })

  it.each([
    ['importRecord.keepFrp', 'KEEP_FRP'],
    ['importRecord.useSource', 'USE_SOURCE'],
  ] as const)('resolves a conflict with %s', async (buttonName, resolution) => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.resolveConflict).mockResolvedValue({})
    renderReview()

    const conflictRow = await row('2026-09-02')
    expect(within(conflictRow).queryByRole('button', { name: 'importRecord.ignore' })).not.toBeInTheDocument()
    await user.click(within(conflictRow).getByRole('button', { name: buttonName }))

    await waitFor(() => expect(AccountingService.resolveConflict).toHaveBeenCalledWith(7, 2, { resolution }))
  })

  it('shows the error of a failed action and keeps the list', async () => {
    const user = userEvent.setup()
    vi.mocked(AccountingService.retryRecord).mockRejectedValue(apiError('Sync is running', 409))
    renderReview()

    await user.click(within(await row('Bistro - Lunch')).getByRole('button', { name: 'importRecord.retry' }))

    expect(await screen.findByText('Sync is running')).toBeInTheDocument()
    expect(screen.getByText('Bistro - Lunch')).toBeInTheDocument()
    expect(AccountingService.getRecords).toHaveBeenCalledTimes(1)
  })
})
