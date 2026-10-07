import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Label, Select, TextInput } from 'flowbite-react'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccConnectionDto } from '../../../../api/models/AccConnectionDto'
import type { AccConnectorDto } from '../../../../api/models/AccConnectorDto'
import { ErrorDisplay } from '../../../UIComponent/ErrorDisplay'
import { InputNumber, InputText } from '../../../UIComponent/Input'
import { H3Title, Paragraph } from '../../../UIComponent/Text'
import { useApiAction } from '../accountingUtils'

export const DEFAULT_SYNC_INTERVAL_MINUTES = 360
export const IMPORT_START_DATE_SETTING = 'importStartDate'

interface ConnectionFormProps {
  connectors: AccConnectorDto[]
  connection?: AccConnectionDto
  onSaved: () => void
  onCancel: () => void
}

const filledValues = (values: Record<string, string>) =>
  Object.fromEntries(Object.entries(values).filter(([, value]) => value.trim() !== ''))

export const ConnectionForm: React.FC<ConnectionFormProps> = ({ connectors, connection, onSaved, onCancel }) => {
  const { t } = useTranslation()
  const connectionId = connection?.id
  const [connectorType, setConnectorType] = useState(connection?.connectorType ?? connectors[0]?.type ?? '')
  const [name, setName] = useState(connection?.name ?? '')
  const [syncInterval, setSyncInterval] = useState(
    String(connection?.syncIntervalMinutes ?? DEFAULT_SYNC_INTERVAL_MINUTES),
  )
  const [importStartDate, setImportStartDate] = useState(connection?.syncSettings?.[IMPORT_START_DATE_SETTING] ?? '')
  const [credentials, setCredentials] = useState<Record<string, string>>({})
  const { busy, error, run } = useApiAction('connection.error')

  const credentialFields = connectors.find((connector) => connector.type === connectorType)?.credentialFields ?? []
  const missingCredential = credentialFields.some((field) => !credentials[field.name ?? '']?.trim())
  const syncIntervalMinutes = Number(syncInterval)
  const canSubmit =
    name.trim() !== '' &&
    connectorType !== '' &&
    syncIntervalMinutes > 0 &&
    (connectionId !== undefined || !missingCredential)

  const syncSettings = () => {
    const otherSettings = Object.entries(connection?.syncSettings ?? {}).filter(
      ([key]) => key !== IMPORT_START_DATE_SETTING,
    )
    return Object.fromEntries(
      importStartDate ? [...otherSettings, [IMPORT_START_DATE_SETTING, importStartDate]] : otherSettings,
    )
  }

  const update = async (id: number) => {
    await AccountingService.updateConnection(id, { name, syncSettings: syncSettings(), syncIntervalMinutes })
    const newCredentials = filledValues(credentials)
    if (Object.keys(newCredentials).length > 0) {
      await AccountingService.setCredentials(id, { credentials: newCredentials })
    }
  }

  const create = async () => {
    const created = await AccountingService.createConnection({
      connectorType,
      name,
      credentials: filledValues(credentials),
      syncSettings: syncSettings(),
    })
    if (created.id !== undefined && created.syncIntervalMinutes !== syncIntervalMinutes) {
      await AccountingService.updateConnection(created.id, {
        name,
        syncSettings: created.syncSettings,
        syncIntervalMinutes,
      })
    }
  }

  const save = async () => {
    const saved = await run(() => (connectionId === undefined ? create() : update(connectionId)))
    if (saved) onSaved()
  }

  return (
    <div className="space-y-4">
      <H3Title>{connectionId === undefined ? t('connection.createTitle') : t('connection.editTitle')}</H3Title>
      <div>
        <Label htmlFor="connectorType">{t('connection.connectorType')}</Label>
        <Select
          id="connectorType"
          value={connectorType}
          disabled={connectionId !== undefined}
          onChange={(event) => {
            setConnectorType(event.target.value)
            setCredentials({})
          }}
        >
          {connectors.map((connector) => (
            <option key={connector.type} value={connector.type}>
              {t(`connection.connectorTypes.${connector.type}`, { defaultValue: connector.type })}
            </option>
          ))}
        </Select>
      </div>
      <InputText
        id="connectionName"
        name="connectionName"
        labelTranslationKey="connection.name"
        placeholderTranslationKey="connection.name"
        value={name}
        onChange={(event) => setName(event.target.value)}
      />
      {connectionId !== undefined && <Paragraph>{t('connection.credentialsHint')}</Paragraph>}
      {credentialFields.map((field) => (
        <div key={field.name}>
          <Label htmlFor={`credential-${field.name}`}>
            {t(`connection.credentialFields.${field.name}`, { defaultValue: field.name })}
          </Label>
          <TextInput
            id={`credential-${field.name}`}
            type={field.secret ? 'password' : 'text'}
            autoComplete="off"
            value={credentials[field.name ?? ''] ?? ''}
            onChange={(event) => setCredentials({ ...credentials, [field.name ?? '']: event.target.value })}
          />
        </div>
      ))}
      <InputNumber
        id="syncInterval"
        name="syncInterval"
        min={1}
        labelTranslationKey="connection.syncInterval"
        placeholderTranslationKey="connection.syncInterval"
        value={syncInterval}
        onChange={(event) => setSyncInterval(event.target.value)}
      />
      <div>
        <Label htmlFor="importStartDate">{t('connection.importStartDate')}</Label>
        <TextInput
          id="importStartDate"
          type="date"
          value={importStartDate}
          onChange={(event) => setImportStartDate(event.target.value)}
        />
      </div>
      {error && <ErrorDisplay error={error} />}
      <div className="flex justify-end gap-2">
        <Button color="gray" onClick={onCancel}>
          {t('common.close')}
        </Button>
        <Button onClick={save} disabled={busy || !canSubmit}>
          {t('connection.save')}
        </Button>
      </div>
    </div>
  )
}
