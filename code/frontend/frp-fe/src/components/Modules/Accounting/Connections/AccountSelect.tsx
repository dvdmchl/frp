import React from 'react'
import { useTranslation } from 'react-i18next'
import { Select } from 'flowbite-react'
import type { AccAccountDto } from '../../../../api/models/AccAccountDto'

interface AccountSelectProps {
  id: string
  ariaLabel?: string
  accounts: AccAccountDto[]
  value: number | undefined
  disabled?: boolean
  onChange: (accountId: number | undefined) => void
}

export const AccountSelect: React.FC<AccountSelectProps> = ({ id, ariaLabel, accounts, value, disabled, onChange }) => {
  const { t } = useTranslation()
  return (
    <Select
      id={id}
      aria-label={ariaLabel}
      sizing="sm"
      value={value ?? ''}
      disabled={disabled}
      onChange={(event) => onChange(event.target.value ? Number(event.target.value) : undefined)}
    >
      <option value="">{t('common.none')}</option>
      {accounts.map((account) => (
        <option key={account.id} value={account.id}>
          {account.name}
        </option>
      ))}
    </Select>
  )
}
