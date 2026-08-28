import React from 'react'
import { useParams, Navigate } from 'react-router-dom'
import { Paths } from '../../constants/Paths'
import { UserManagementAdmin } from './UserManagementAdmin'
import { MaintenanceAdmin } from './MaintenanceAdmin'

export const AdminPage: React.FC = () => {
  const { section: urlSection } = useParams<{ section: string }>()

  const section = urlSection === 'maintenance' ? 'maintenance' : urlSection === 'users' ? 'users' : null

  if (!urlSection) {
    return <Navigate to={Paths.ADMIN_USERS} replace />
  }

  return (
    <div className="min-w-0">
      {section === 'users' && <UserManagementAdmin />}
      {section === 'maintenance' && <MaintenanceAdmin />}
    </div>
  )
}
