import React, { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow } from 'flowbite-react'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccImportRecordDto } from '../../../../api/models/AccImportRecordDto'
import { ErrorDisplay } from '../../../UIComponent/ErrorDisplay'
import { useApiAction } from '../accountingUtils'
import { ImportRecordDetail } from './ImportRecordDetail'

type RecordStatus = NonNullable<AccImportRecordDto['status']>

const REVIEWED_STATUSES: RecordStatus[] = ['ERROR', 'CONFLICT', 'NEW']
const STATUS_COLORS: Partial<Record<RecordStatus, string>> = {
  ERROR: 'failure',
  CONFLICT: 'warning',
  NEW: 'info',
}

interface ImportRecordReviewProps {
  connectionId: number
  refreshKey: number
}

export const ImportRecordReview: React.FC<ImportRecordReviewProps> = ({ connectionId, refreshKey }) => {
  const { t } = useTranslation()
  const [records, setRecords] = useState<AccImportRecordDto[]>([])
  const [detailId, setDetailId] = useState<number | null>(null)
  const { busy, error, run } = useApiAction('importRecord.error')

  const load = useCallback(
    () => run(async () => setRecords(await AccountingService.getRecords(connectionId, REVIEWED_STATUSES))),
    [connectionId, run],
  )

  useEffect(() => {
    load()
  }, [load, refreshKey])

  const act = async (action: () => Promise<unknown>) => {
    if (await run(action)) await load()
  }

  const actions = (record: AccImportRecordDto) => {
    const recordId = record.id ?? 0
    const ignore = (
      <Button size="xs" color="light" onClick={() => act(() => AccountingService.ignoreRecord(connectionId, recordId))}>
        {t('importRecord.ignore')}
      </Button>
    )
    switch (record.status) {
      case 'ERROR':
        return (
          <>
            <Button size="xs" onClick={() => act(() => AccountingService.retryRecord(connectionId, recordId))}>
              {t('importRecord.retry')}
            </Button>
            {ignore}
          </>
        )
      case 'CONFLICT':
        return (
          <>
            <Button
              size="xs"
              color="light"
              onClick={() =>
                act(() => AccountingService.resolveConflict(connectionId, recordId, { resolution: 'KEEP_FRP' }))
              }
            >
              {t('importRecord.keepFrp')}
            </Button>
            <Button
              size="xs"
              onClick={() =>
                act(() => AccountingService.resolveConflict(connectionId, recordId, { resolution: 'USE_SOURCE' }))
              }
            >
              {t('importRecord.useSource')}
            </Button>
          </>
        )
      default:
        return ignore
    }
  }

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
              <TableHeadCell>{t('importRecord.date')}</TableHeadCell>
              <TableHeadCell>{t('importRecord.amount')}</TableHeadCell>
              <TableHeadCell>{t('importRecord.description')}</TableHeadCell>
              <TableHeadCell>{t('importRecord.status')}</TableHeadCell>
              <TableHeadCell>{t('importRecord.reason')}</TableHeadCell>
              <TableHeadCell>{t('common.actions')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {records.map((record) => (
              <TableRow
                key={record.id}
                className="cursor-pointer bg-white hover:bg-gray-50"
                onClick={() => setDetailId(record.id ?? null)}
              >
                <TableCell>{record.recordDate}</TableCell>
                <TableCell>{[record.amount?.toFixed(2), record.currencyCode].filter(Boolean).join(' ')}</TableCell>
                <TableCell>{[record.counterparty, record.note].filter(Boolean).join(' - ') || '—'}</TableCell>
                <TableCell>
                  {record.status && (
                    <Badge color={STATUS_COLORS[record.status]}>{t(`importRecord.statuses.${record.status}`)}</Badge>
                  )}
                </TableCell>
                <TableCell>
                  <button type="button" className="text-left text-blue-700 hover:underline">
                    {record.errorMessage ?? t(`importRecord.reasons.${record.status}`)}
                  </button>
                </TableCell>
                <TableCell onClick={(event) => event.stopPropagation()}>
                  <fieldset disabled={busy} className="flex flex-wrap gap-2">
                    {actions(record)}
                  </fieldset>
                </TableCell>
              </TableRow>
            ))}
            {records.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-6 text-center text-gray-500">
                  {t('importRecord.empty')}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      {detailId !== null && (
        <ImportRecordDetail connectionId={connectionId} recordId={detailId} onClose={() => setDetailId(null)} />
      )}
    </div>
  )
}
