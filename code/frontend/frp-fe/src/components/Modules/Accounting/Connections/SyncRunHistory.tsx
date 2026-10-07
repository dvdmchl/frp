import React, { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow } from 'flowbite-react'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccSyncRunDto } from '../../../../api/models/AccSyncRunDto'
import { ErrorDisplay } from '../../../UIComponent/ErrorDisplay'
import { formatDateTime, useApiAction } from '../accountingUtils'

const STATUS_COLORS: Record<NonNullable<AccSyncRunDto['status']>, string> = {
  RUNNING: 'info',
  SUCCESS: 'success',
  PARTIAL: 'warning',
  FAILED: 'failure',
}
const COUNTS = ['fetched', 'created', 'updated', 'deleted', 'posted', 'errors'] as const

interface SyncRunHistoryProps {
  connectionId: number
  refreshKey: number
}

export const SyncRunHistory: React.FC<SyncRunHistoryProps> = ({ connectionId, refreshKey }) => {
  const { t } = useTranslation()
  const [runs, setRuns] = useState<AccSyncRunDto[]>([])
  const { busy, error, run } = useApiAction('syncRun.error')

  const load = useCallback(
    () => run(async () => setRuns(await AccountingService.getRuns(connectionId))),
    [connectionId, run],
  )

  useEffect(() => {
    load()
  }, [load, refreshKey])

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <Button color="light" size="sm" disabled={busy} onClick={load}>
          {t('common.reload')}
        </Button>
      </div>
      {error && <ErrorDisplay error={error} />}
      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('syncRun.startedAt')}</TableHeadCell>
              <TableHeadCell>{t('syncRun.finishedAt')}</TableHeadCell>
              <TableHeadCell>{t('syncRun.trigger')}</TableHeadCell>
              <TableHeadCell>{t('syncRun.status')}</TableHeadCell>
              {COUNTS.map((count) => (
                <TableHeadCell key={count}>{t(`syncRun.counts.${count}`)}</TableHeadCell>
              ))}
              <TableHeadCell>{t('syncRun.errorMessage')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {runs.map((syncRun) => (
              <TableRow key={syncRun.id} className="bg-white">
                <TableCell>{formatDateTime(syncRun.startedAt)}</TableCell>
                <TableCell>{formatDateTime(syncRun.finishedAt)}</TableCell>
                <TableCell>{t(`syncRun.triggers.${syncRun.trigger}`)}</TableCell>
                <TableCell>
                  {syncRun.status && (
                    <Badge color={STATUS_COLORS[syncRun.status]}>{t(`syncRun.statuses.${syncRun.status}`)}</Badge>
                  )}
                </TableCell>
                {COUNTS.map((count) => (
                  <TableCell key={count}>{syncRun[count] ?? 0}</TableCell>
                ))}
                <TableCell>{syncRun.errorMessage ?? '—'}</TableCell>
              </TableRow>
            ))}
            {runs.length === 0 && (
              <TableRow>
                <TableCell colSpan={COUNTS.length + 5} className="py-6 text-center text-gray-500">
                  {t('syncRun.empty')}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
