import React from 'react'
import { Routes, Route, Navigate, Link } from 'react-router-dom'
import { CurrencyManager } from './CurrencyManager'
import { AccountTree } from './AccountTree'
import { AccountDetailPage } from './AccountDetailPage'
import { Button } from 'flowbite-react'
import { useTranslation } from 'react-i18next'
import { Paths } from '../../../constants/Paths'
import { ConnectionsPage } from './Connections/ConnectionsPage'
import { ConnectionDetailPage } from './Connections/ConnectionDetailPage'

const AccountingDashboard: React.FC = () => {
  const { t } = useTranslation()
  return (
    <div className="p-4">
      <div className="flex justify-end gap-2 mb-4">
        <Link to={Paths.ACCOUNTING_CONNECTIONS}>
          <Button color="light">{t('connection.title')}</Button>
        </Link>
        <Link to={Paths.ACCOUNTING_CURRENCIES}>
          <Button color="light">{t('currency.title')}</Button>
        </Link>
      </div>
      <AccountTree />
    </div>
  )
}

export const AccountingModule: React.FC = () => {
  return (
    <Routes>
      <Route index element={<AccountingDashboard />} />
      <Route path={Paths.ACCOUNTING_CURRENCIES} element={<CurrencyManager />} />
      <Route path={Paths.ACCOUNTING_ACCOUNT} element={<AccountDetailPage />} />
      <Route path={Paths.ACCOUNTING_CONNECTIONS} element={<ConnectionsPage />} />
      <Route path={Paths.ACCOUNTING_CONNECTION} element={<ConnectionDetailPage />} />
      <Route path={Paths.WILDCARD} element={<Navigate to={Paths.CURRENT} replace />} />
    </Routes>
  )
}
