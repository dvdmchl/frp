import React, { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Badge,
  Button,
  Modal,
  ModalBody,
  Spinner,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from 'flowbite-react'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccConnectionDto } from '../../../../api/models/AccConnectionDto'
import type { AccConnectorDto } from '../../../../api/models/AccConnectorDto'
import { ErrorDisplay } from '../../../UIComponent/ErrorDisplay'
import { H2Title, LinkText, TextSuccess } from '../../../UIComponent/Text'
import { Paths } from '../../../../constants/Paths'
import { formatDateTime, useApiAction } from '../accountingUtils'
import { ConnectionForm } from './ConnectionForm'

const connectionState = (connection: AccConnectionDto) => {
  if (connection.credentialsRejected) return { key: 'connection.state.credentialsRejected', color: 'failure' }
  if (!connection.credentialsSet) return { key: 'connection.state.noCredentials', color: 'warning' }
  if (!connection.enabled) return { key: 'connection.state.disabled', color: 'gray' }
  return { key: 'connection.state.active', color: 'success' }
}

export const ConnectionsPage: React.FC = () => {
  const { t } = useTranslation()
  const [connections, setConnections] = useState<AccConnectionDto[]>([])
  const [connectors, setConnectors] = useState<AccConnectorDto[]>([])
  const [loading, setLoading] = useState(true)
  const [success, setSuccess] = useState<string | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editedConnection, setEditedConnection] = useState<AccConnectionDto | undefined>()
  const { busy, error, run } = useApiAction('connection.error')

  const load = useCallback(async () => {
    await run(async () => {
      const [connectionList, connectorList] = await Promise.all([
        AccountingService.getConnections(),
        AccountingService.getConnectors(),
      ])
      setConnections(connectionList)
      setConnectors(connectorList)
    })
    setLoading(false)
  }, [run])

  useEffect(() => {
    load()
  }, [load])

  const runAction = async (action: () => Promise<unknown>, successKey: string) => {
    setSuccess(null)
    if (await run(action)) {
      setSuccess(t(successKey))
      await load()
    }
  }

  const openForm = (connection?: AccConnectionDto) => {
    setEditedConnection(connection)
    setSuccess(null)
    setFormOpen(true)
  }

  const handleSaved = async () => {
    setFormOpen(false)
    setSuccess(t('connection.saveSuccess'))
    await load()
  }

  const handleDelete = (id: number) => {
    if (!globalThis.confirm(t('connection.deleteConfirm'))) return
    runAction(() => AccountingService.deleteConnection(id), 'connection.deleteSuccess')
  }

  if (loading) {
    return (
      <div className="flex justify-center p-12">
        <Spinner size="xl" />
      </div>
    )
  }

  return (
    <div className="space-y-4 p-4">
      <LinkText to={Paths.PARENT}>← {t('connection.backToAccounting')}</LinkText>
      <H2Title>{t('connection.title')}</H2Title>
      <div className="flex justify-end">
        <Button onClick={() => openForm()} disabled={connectors.length === 0}>
          {t('connection.createTitle')}
        </Button>
      </div>

      {error && <ErrorDisplay error={error} />}
      {success && <TextSuccess message={success} />}

      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('connection.name')}</TableHeadCell>
              <TableHeadCell>{t('connection.connectorType')}</TableHeadCell>
              <TableHeadCell>{t('connection.stateTitle')}</TableHeadCell>
              <TableHeadCell>{t('connection.lastSync')}</TableHeadCell>
              <TableHeadCell>{t('connection.nextSync')}</TableHeadCell>
              <TableHeadCell>{t('common.actions')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {connections.map((connection) => {
              const id = connection.id ?? 0
              const state = connectionState(connection)
              return (
                <TableRow key={id} className="bg-white">
                  <TableCell className="font-medium text-gray-900">
                    <LinkText to={String(id)}>{connection.name}</LinkText>
                  </TableCell>
                  <TableCell>
                    {t(`connection.connectorTypes.${connection.connectorType}`, {
                      defaultValue: connection.connectorType,
                    })}
                  </TableCell>
                  <TableCell>
                    <Badge color={state.color}>{t(state.key)}</Badge>
                  </TableCell>
                  <TableCell>{formatDateTime(connection.lastSuccessfulSyncAt)}</TableCell>
                  <TableCell>{formatDateTime(connection.nextSyncAt)}</TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-2">
                      <Button size="xs" color="light" onClick={() => openForm(connection)}>
                        {t('common.edit')}
                      </Button>
                      <Button
                        size="xs"
                        color="light"
                        disabled={busy}
                        onClick={() =>
                          runAction(
                            () => AccountingService.setEnabled(id, { enabled: !connection.enabled }),
                            connection.enabled ? 'connection.disableSuccess' : 'connection.enableSuccess',
                          )
                        }
                      >
                        {connection.enabled ? t('connection.disable') : t('connection.enable')}
                      </Button>
                      <Button
                        size="xs"
                        color="light"
                        disabled={busy}
                        onClick={() => runAction(() => AccountingService.testConnection(id), 'connection.testSuccess')}
                      >
                        {t('connection.test')}
                      </Button>
                      <Button
                        size="xs"
                        disabled={busy || !connection.enabled}
                        onClick={() => runAction(() => AccountingService.syncNow(id), 'connection.syncStarted')}
                      >
                        {t('connection.syncNow')}
                      </Button>
                      <Button size="xs" color="failure" disabled={busy} onClick={() => handleDelete(id)}>
                        {t('common.delete')}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              )
            })}
            {connections.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-8 text-center text-gray-500">
                  {t('connection.empty')}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <Modal show={formOpen} onClose={() => setFormOpen(false)}>
        <ModalBody>
          <ConnectionForm
            key={editedConnection?.id ?? 'create'}
            connectors={connectors}
            connection={editedConnection}
            onSaved={handleSaved}
            onCancel={() => setFormOpen(false)}
          />
        </ModalBody>
      </Modal>
    </div>
  )
}
