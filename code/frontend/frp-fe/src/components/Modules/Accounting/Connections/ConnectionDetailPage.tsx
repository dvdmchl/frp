import React, { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Button, Spinner, TabItem, Tabs } from 'flowbite-react'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccConnectionDto } from '../../../../api/models/AccConnectionDto'
import { ErrorDisplay } from '../../../UIComponent/ErrorDisplay'
import { H2Title, LinkText, TextSuccess } from '../../../UIComponent/Text'
import { Paths } from '../../../../constants/Paths'
import { useApiAction } from '../accountingUtils'
import { ConnectionMappings } from './ConnectionMappings'
import { ImportRecordReview } from './ImportRecordReview'
import { SyncRunHistory } from './SyncRunHistory'

export const ConnectionDetailPage: React.FC = () => {
  const { t } = useTranslation()
  const connectionId = Number(useParams().connectionId)
  const [connection, setConnection] = useState<AccConnectionDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [syncStarted, setSyncStarted] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const { busy, error, run } = useApiAction('connection.error')

  const load = useCallback(async () => {
    await run(async () => setConnection(await AccountingService.getConnection(connectionId)))
    setLoading(false)
  }, [connectionId, run])

  useEffect(() => {
    load()
  }, [load])

  const syncNow = async () => {
    setSyncStarted(false)
    if (await run(() => AccountingService.syncNow(connectionId))) {
      setSyncStarted(true)
      setRefreshKey((key) => key + 1)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center p-12">
        <Spinner size="xl" />
      </div>
    )
  }

  const backLink = (
    <LinkText to={`${Paths.PARENT}/${Paths.ACCOUNTING_CONNECTIONS}`}>← {t('connection.backToList')}</LinkText>
  )

  if (!connection) {
    return (
      <div className="space-y-4 p-4">
        <ErrorDisplay error={error ?? { message: t('connection.notFound') }} />
        {backLink}
      </div>
    )
  }

  return (
    <div className="space-y-4 p-4">
      {backLink}
      <H2Title>{connection.name}</H2Title>
      <div className="flex justify-end">
        <Button disabled={busy || !connection.enabled} onClick={syncNow}>
          {t('connection.syncNow')}
        </Button>
      </div>
      {error && <ErrorDisplay error={error} />}
      {syncStarted && <TextSuccess message={t('connection.syncStarted')} />}
      <Tabs variant="underline">
        <TabItem active title={t('connection.tabs.records')}>
          <ImportRecordReview connectionId={connectionId} refreshKey={refreshKey} />
        </TabItem>
        <TabItem title={t('connection.tabs.mappings')}>
          <ConnectionMappings connection={connection} onConnectionChange={setConnection} />
        </TabItem>
        <TabItem title={t('connection.tabs.runs')}>
          <SyncRunHistory connectionId={connectionId} refreshKey={refreshKey} />
        </TabItem>
      </Tabs>
    </div>
  )
}
