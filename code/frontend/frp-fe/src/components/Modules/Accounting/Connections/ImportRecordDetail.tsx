import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Modal, ModalBody, ModalFooter, ModalHeader, Spinner } from 'flowbite-react'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccImportRecordDetailDto } from '../../../../api/models/AccImportRecordDetailDto'
import type { AccMappedItemDto } from '../../../../api/models/AccMappedItemDto'
import { ErrorDisplay } from '../../../UIComponent/ErrorDisplay'
import { formatDateTime, useApiAction } from '../accountingUtils'

interface ImportRecordDetailProps {
  connectionId: number
  recordId: number
  onClose: () => void
}

const formatAmount = (amount: number | undefined, currencyCode: string | undefined) =>
  amount === undefined ? undefined : [amount.toFixed(2), currencyCode].filter(Boolean).join(' ')

/**
 * Raw payload pretty-printed; kept as it is when it is not valid JSON.
 */
const formatPayload = (payload: string) => {
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch {
    return payload
  }
}

/**
 * Popup with the detail of a staged record: what it is in the source, how it is mapped and its raw data.
 */
export const ImportRecordDetail: React.FC<ImportRecordDetailProps> = ({ connectionId, recordId, onClose }) => {
  const { t } = useTranslation()
  const [detail, setDetail] = useState<AccImportRecordDetailDto | null>(null)
  const { busy, error, run } = useApiAction('importRecord.error')

  useEffect(() => {
    run(async () => setDetail(await AccountingService.getRecordDetail(connectionId, recordId)))
  }, [connectionId, recordId, run])

  const mapped = (item: AccMappedItemDto | undefined, missingKey: string) =>
    item
      ? `${item.externalName ?? item.externalId} → ${item.accountName ?? t('importRecord.detail.notMapped')}`
      : t(missingKey)

  const rows = (value: AccImportRecordDetailDto): [string, string | undefined][] => {
    const { importRecord: record } = value
    if (!record) {
      return []
    }
    return [
      ['importRecord.date', record.recordDate],
      ['importRecord.amount', formatAmount(record.amount, record.currencyCode)],
      ['importRecord.detail.baseAmount', formatAmount(value.baseAmount, value.baseCurrencyCode)],
      ['importRecord.detail.counterparty', record.counterparty],
      ['importRecord.detail.note', record.note],
      ['importRecord.detail.account', mapped(value.account, 'importRecord.detail.unknown')],
      record.transferLinkId
        ? ['importRecord.detail.transferAccount', mapped(value.transferAccount, 'importRecord.detail.otherLegMissing')]
        : ['importRecord.detail.category', mapped(value.category, 'importRecord.detail.uncategorized')],
      ['importRecord.status', record.status && t(`importRecord.statuses.${record.status}`)],
      ['importRecord.reason', record.errorMessage],
      ['importRecord.detail.sourceState', record.sourceState],
      ['importRecord.detail.externalId', record.externalId],
      ['importRecord.detail.sourceUpdatedAt', value.sourceUpdatedAt && formatDateTime(value.sourceUpdatedAt)],
      ['importRecord.detail.firstSeenAt', formatDateTime(value.firstSeenAt)],
      ['importRecord.detail.lastSeenAt', formatDateTime(record.lastSeenAt)],
    ]
  }

  return (
    <Modal show size="2xl" onClose={onClose}>
      <ModalHeader>{t('importRecord.detail.title')}</ModalHeader>
      <ModalBody>
        {error && <ErrorDisplay error={error} />}
        {busy && <Spinner aria-label={t('importRecord.detail.loading')} />}
        {detail && (
          <div className="space-y-4">
            <dl className="grid grid-cols-1 gap-x-4 gap-y-2 text-sm sm:grid-cols-3">
              {rows(detail)
                .filter(([, value]) => value)
                .map(([labelKey, value]) => (
                  <React.Fragment key={labelKey}>
                    <dt className="font-medium text-gray-500">{t(labelKey)}</dt>
                    <dd className="break-words text-gray-900 sm:col-span-2">{value}</dd>
                  </React.Fragment>
                ))}
            </dl>
            {detail.rawPayload && (
              <details>
                <summary className="cursor-pointer text-sm font-medium text-gray-700">
                  {t('importRecord.detail.rawPayload')}
                </summary>
                <pre className="mt-2 max-h-80 overflow-auto rounded bg-gray-50 p-3 text-xs">
                  {formatPayload(detail.rawPayload)}
                </pre>
              </details>
            )}
          </div>
        )}
      </ModalBody>
      <ModalFooter>
        <Button color="gray" onClick={onClose}>
          {t('common.close')}
        </Button>
      </ModalFooter>
    </Modal>
  )
}
