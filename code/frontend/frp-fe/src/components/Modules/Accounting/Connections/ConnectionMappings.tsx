import React, { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Label } from 'flowbite-react'
import { AccountingService } from '../../../../api/services/AccountingService'
import type { AccAccountDto } from '../../../../api/models/AccAccountDto'
import type { AccConnectionDto } from '../../../../api/models/AccConnectionDto'
import type { AccExternalMappingDto } from '../../../../api/models/AccExternalMappingDto'
import type { AccExternalMappingUpdateRequestDto } from '../../../../api/models/AccExternalMappingUpdateRequestDto'
import type { AccNodeDto } from '../../../../api/models/AccNodeDto'
import { ErrorDisplay } from '../../../UIComponent/ErrorDisplay'
import { H3Title } from '../../../UIComponent/Text'
import { flattenNodes, useApiAction } from '../accountingUtils'
import { AccountSelect } from './AccountSelect'
import { MappingTable, type MappingKind } from './MappingTable'

const ACCOUNT_TYPES_BY_KIND: Record<MappingKind, AccAccountDto['accountType'][]> = {
  ACCOUNT: ['ASSET', 'LIABILITY'],
  CATEGORY: ['EXPENSE', 'REVENUE'],
}
const MAPPING_KINDS: MappingKind[] = ['ACCOUNT', 'CATEGORY']

const replaceMapping = (mappings: AccExternalMappingDto[], updated: AccExternalMappingDto) =>
  mappings.map((mapping) => (mapping.id === updated.id ? updated : mapping))

interface ConnectionMappingsProps {
  connection: AccConnectionDto
  onConnectionChange: (connection: AccConnectionDto) => void
}

export const ConnectionMappings: React.FC<ConnectionMappingsProps> = ({ connection, onConnectionChange }) => {
  const { t } = useTranslation()
  const connectionId = connection.id ?? 0
  const [mappings, setMappings] = useState<AccExternalMappingDto[]>([])
  const [nodes, setNodes] = useState<AccNodeDto[]>([])
  const [fallbackExpenseId, setFallbackExpenseId] = useState(connection.fallbackExpenseAccountId)
  const [fallbackRevenueId, setFallbackRevenueId] = useState(connection.fallbackRevenueAccountId)
  const { busy, error, run } = useApiAction('mapping.error')

  const load = useCallback(
    () =>
      run(async () => {
        const [mappingList, tree] = await Promise.all([
          AccountingService.getMappings(connectionId),
          AccountingService.getTree(),
        ])
        setMappings(mappingList)
        setNodes(flattenNodes(tree))
      }),
    [connectionId, run],
  )

  useEffect(() => {
    load()
  }, [load])

  const postableAccounts = nodes.flatMap((node) => (!node.isPlaceholder && node.account ? [node.account] : []))
  const accountsOfTypes = (types: AccAccountDto['accountType'][]) =>
    postableAccounts.filter((account) => types.includes(account.accountType))
  const parentNodes = nodes.flatMap((node) =>
    node.id !== undefined && node.account?.name ? [{ id: node.id, name: node.account.name }] : [],
  )

  const updateMapping = (mapping: AccExternalMappingDto, request: AccExternalMappingUpdateRequestDto) =>
    run(async () => {
      const updated = await AccountingService.updateMapping(connectionId, mapping.id ?? 0, request)
      setMappings((current) => replaceMapping(current, updated))
    })

  const createMissing = async (kind: MappingKind, parentNodeId: number | undefined) => {
    const created = await run(() => AccountingService.createMissingAccounts(connectionId, { kind, parentNodeId }))
    if (created) await load()
  }

  const refresh = () => run(async () => setMappings(await AccountingService.refreshMappings(connectionId)))

  const saveFallback = () =>
    run(async () =>
      onConnectionChange(
        await AccountingService.setFallbackAccounts(connectionId, {
          expenseAccountId: fallbackExpenseId,
          revenueAccountId: fallbackRevenueId,
        }),
      ),
    )

  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <Button color="light" disabled={busy} onClick={refresh}>
          {t('mapping.refresh')}
        </Button>
      </div>
      {error && <ErrorDisplay error={error} />}
      {MAPPING_KINDS.map((kind) => (
        <MappingTable
          key={kind}
          kind={kind}
          mappings={mappings.filter((mapping) => mapping.kind === kind)}
          accounts={accountsOfTypes(ACCOUNT_TYPES_BY_KIND[kind])}
          parentNodes={parentNodes}
          busy={busy}
          onUpdate={updateMapping}
          onCreateMissing={createMissing}
        />
      ))}
      <div className="space-y-3">
        <H3Title>{t('mapping.fallbackTitle')}</H3Title>
        <div className="grid gap-3 md:grid-cols-2">
          <div>
            <Label htmlFor="fallback-expense">{t('mapping.fallbackExpense')}</Label>
            <AccountSelect
              id="fallback-expense"
              accounts={accountsOfTypes(['EXPENSE'])}
              value={fallbackExpenseId}
              onChange={setFallbackExpenseId}
            />
          </div>
          <div>
            <Label htmlFor="fallback-revenue">{t('mapping.fallbackRevenue')}</Label>
            <AccountSelect
              id="fallback-revenue"
              accounts={accountsOfTypes(['REVENUE'])}
              value={fallbackRevenueId}
              onChange={setFallbackRevenueId}
            />
          </div>
        </div>
        <div className="flex justify-end">
          <Button disabled={busy} onClick={saveFallback}>
            {t('mapping.saveFallback')}
          </Button>
        </div>
      </div>
    </div>
  )
}
