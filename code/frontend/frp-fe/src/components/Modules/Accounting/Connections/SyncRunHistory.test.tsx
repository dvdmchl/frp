import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountingService } from '../../../../api/services/AccountingService'
import { apiError } from '../../../../test/apiError'
import { RUNNING_REFRESH_MS, SyncRunHistory } from './SyncRunHistory'

vi.mock('../../../../api/services/AccountingService')

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

describe('SyncRunHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(AccountingService.getRuns).mockResolvedValue([
      {
        id: 2,
        trigger: 'MANUAL',
        status: 'PARTIAL',
        startedAt: '2026-10-01T08:00:00Z',
        finishedAt: '2026-10-01T08:01:00Z',
        fetched: 12,
        created: 3,
        updated: 1,
        deleted: 0,
        posted: 2,
        errors: 1,
      },
      { id: 1, trigger: 'SCHEDULED', status: 'FAILED', startedAt: '2026-09-30T08:00:00Z', errorMessage: 'Timeout' },
    ])
  })

  it('lists runs with trigger, status and counts', async () => {
    render(<SyncRunHistory connectionId={7} refreshKey={0} />)

    const partialRun = (await screen.findByText('syncRun.statuses.PARTIAL')).closest('tr') as HTMLElement
    expect(within(partialRun).getByText('syncRun.triggers.MANUAL')).toBeInTheDocument()
    expect(within(partialRun).getByText('12')).toBeInTheDocument()
    const failedRun = screen.getByText('syncRun.statuses.FAILED').closest('tr') as HTMLElement
    expect(within(failedRun).getByText('Timeout')).toBeInTheDocument()
    expect(AccountingService.getRuns).toHaveBeenCalledWith(7)
  })

  it('shows an empty message without runs', async () => {
    vi.mocked(AccountingService.getRuns).mockResolvedValue([])
    render(<SyncRunHistory connectionId={7} refreshKey={0} />)

    expect(await screen.findByText('syncRun.empty')).toBeInTheDocument()
  })

  it('reloads the runs on demand and when the refresh key changes', async () => {
    const user = userEvent.setup()
    const { rerender } = render(<SyncRunHistory connectionId={7} refreshKey={0} />)
    await screen.findByText('syncRun.statuses.PARTIAL')

    await user.click(screen.getByRole('button', { name: 'common.reload' }))
    rerender(<SyncRunHistory connectionId={7} refreshKey={1} />)

    await waitFor(() => expect(AccountingService.getRuns).toHaveBeenCalledTimes(3))
  })

  describe('while a run is running', () => {
    const runningRun = { id: 3, trigger: 'MANUAL', status: 'RUNNING', startedAt: '2026-10-02T08:00:00Z' } as const
    const finishedRun = { ...runningRun, status: 'SUCCESS', finishedAt: '2026-10-02T08:00:01Z' } as const

    beforeEach(() => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('reloads the runs until the run finishes and reports its end', async () => {
      const onRunFinished = vi.fn()
      vi.mocked(AccountingService.getRuns).mockResolvedValueOnce([runningRun]).mockResolvedValue([finishedRun])
      render(<SyncRunHistory connectionId={7} refreshKey={0} onRunFinished={onRunFinished} />)
      await screen.findByText('syncRun.statuses.RUNNING')
      expect(onRunFinished).not.toHaveBeenCalled()

      await vi.advanceTimersByTimeAsync(RUNNING_REFRESH_MS)

      expect(await screen.findByText('syncRun.statuses.SUCCESS')).toBeInTheDocument()
      expect(onRunFinished).toHaveBeenCalledTimes(1)
      await vi.advanceTimersByTimeAsync(RUNNING_REFRESH_MS * 2)
      expect(AccountingService.getRuns).toHaveBeenCalledTimes(2)
    })

    it('does not reload the runs when none is running', async () => {
      render(<SyncRunHistory connectionId={7} refreshKey={0} />)
      await screen.findByText('syncRun.statuses.PARTIAL')

      await vi.advanceTimersByTimeAsync(RUNNING_REFRESH_MS * 2)

      expect(AccountingService.getRuns).toHaveBeenCalledTimes(1)
    })
  })

  it('shows the API error when loading fails', async () => {
    vi.mocked(AccountingService.getRuns).mockRejectedValue(apiError('Source unavailable', 502))
    render(<SyncRunHistory connectionId={7} refreshKey={0} />)

    expect(await screen.findByText('Source unavailable')).toBeInTheDocument()
  })
})
