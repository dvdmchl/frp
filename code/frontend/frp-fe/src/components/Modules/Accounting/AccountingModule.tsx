import React from 'react'
import { Routes, Route, Navigate, Link } from 'react-router-dom'
import { CurrencyManager } from './CurrencyManager'
import { H2Title } from '../../UIComponent/Text'
import { Button } from 'flowbite-react'

const AccountingDashboard: React.FC = () => {
    return (
        <div className="p-4">
             <H2Title>Accounting Module</H2Title>
             <div className="flex gap-4 mt-4">
                <Link to="currencies">
                    <Button>Manage Currencies</Button>
                </Link>
             </div>
        </div>
    )
}

export const AccountingModule: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<AccountingDashboard />} />
      <Route path="currencies" element={<CurrencyManager />} />
      <Route path="*" element={<Navigate to="" replace />} />
    </Routes>
  )
}
