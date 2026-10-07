import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  Checkbox,
  Label,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from 'flowbite-react'
import type { AccAccountDto } from '../../../../api/models/AccAccountDto'
import type { AccExternalMappingDto } from '../../../../api/models/AccExternalMappingDto'
import type { AccExternalMappingUpdateRequestDto } from '../../../../api/models/AccExternalMappingUpdateRequestDto'
import { H3Title } from '../../../UIComponent/Text'
import { AccountSelect } from './AccountSelect'

export type MappingKind = NonNullable<AccExternalMappingDto['kind']>

interface MappingTableProps {
  kind: MappingKind
  mappings: AccExternalMappingDto[]
  accounts: AccAccountDto[]
  parentNodes: { id: number; name: string }[]
  busy: boolean
  onUpdate: (mapping: AccExternalMappingDto, request: AccExternalMappingUpdateRequestDto) => void
  onCreateMissing: (kind: MappingKind, parentNodeId: number | undefined) => void
}

export const MappingTable: React.FC<MappingTableProps> = ({
  kind,
  mappings,
  accounts,
  parentNodes,
  busy,
  onUpdate,
  onCreateMissing,
}) => {
  const { t } = useTranslation()
  const [parentNodeId, setParentNodeId] = useState('')
  const hasMissing = mappings.some((mapping) => !mapping.ignored && !mapping.accountId)
  const parentSelectId = `parent-${kind}`

  return (
    <div className="space-y-3">
      <H3Title>{t(`mapping.kinds.${kind}`)}</H3Title>
      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('mapping.externalName')}</TableHeadCell>
              <TableHeadCell>{t('mapping.currency')}</TableHeadCell>
              <TableHeadCell>{t('mapping.account')}</TableHeadCell>
              <TableHeadCell>{t('mapping.ignored')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {mappings.map((mapping) => (
              <TableRow key={mapping.id} className="bg-white">
                <TableCell className="font-medium text-gray-900">{mapping.externalName}</TableCell>
                <TableCell>{mapping.currencyCode ?? '—'}</TableCell>
                <TableCell>
                  <AccountSelect
                    id={`mapping-${mapping.id}`}
                    ariaLabel={`${t('mapping.account')} ${mapping.externalName}`}
                    accounts={accounts}
                    value={mapping.accountId}
                    disabled={busy || mapping.ignored}
                    onChange={(accountId) => onUpdate(mapping, { accountId, ignored: false })}
                  />
                </TableCell>
                <TableCell>
                  <Checkbox
                    aria-label={`${t('mapping.ignored')} ${mapping.externalName}`}
                    checked={Boolean(mapping.ignored)}
                    disabled={busy}
                    onChange={(event) =>
                      onUpdate(mapping, { accountId: mapping.accountId, ignored: event.target.checked })
                    }
                  />
                </TableCell>
              </TableRow>
            ))}
            {mappings.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="py-6 text-center text-gray-500">
                  {t('mapping.empty')}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      <div className="flex flex-wrap items-end gap-2">
        <div>
          <Label htmlFor={parentSelectId}>{t('mapping.parentNode')}</Label>
          <Select
            id={parentSelectId}
            sizing="sm"
            value={parentNodeId}
            onChange={(event) => setParentNodeId(event.target.value)}
          >
            <option value="">{t('common.none')}</option>
            {parentNodes.map((node) => (
              <option key={node.id} value={node.id}>
                {node.name}
              </option>
            ))}
          </Select>
        </div>
        <Button
          size="sm"
          color="light"
          disabled={busy || !hasMissing}
          onClick={() => onCreateMissing(kind, parentNodeId ? Number(parentNodeId) : undefined)}
        >
          {t('mapping.createMissing')}
        </Button>
      </div>
    </div>
  )
}
