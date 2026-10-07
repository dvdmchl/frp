import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { CurrencyManager } from './CurrencyManager'
import { AccountTree } from './AccountTree'
import { AccountDetailPage } from './AccountDetailPage'
import { Paths } from '../../../constants/Paths'
import { ConnectionsPage } from './Connections/ConnectionsPage'
import { ConnectionDetailPage } from './Connections/ConnectionDetailPage'

export const AccountingModule: React.FC = () => {
  return (
    <Routes>
      <Route
        index
        element={
          <div className="p-4">
            <AccountTree />
          </div>
        }
      />
      <Route path={Paths.ACCOUNTING_CURRENCIES} element={<CurrencyManager />} />
      <Route path={Paths.ACCOUNTING_ACCOUNT} element={<AccountDetailPage />} />
      <Route path={Paths.ACCOUNTING_CONNECTIONS} element={<ConnectionsPage />} />
      <Route path={Paths.ACCOUNTING_CONNECTION} element={<ConnectionDetailPage />} />
      <Route path={Paths.WILDCARD} element={<Navigate to={Paths.CURRENT} replace />} />
    </Routes>
  )
}
