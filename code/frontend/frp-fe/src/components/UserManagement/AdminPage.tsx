import React from 'react'
import { useParams, Navigate } from 'react-router-dom'
import { Paths } from '../../constants/Paths'
import { UserManagementAdmin } from './UserManagementAdmin'
import { MaintenanceAdmin } from './MaintenanceAdmin'

export const AdminPage: React.FC = () => {
  const { section: urlSection } = useParams<{ section: string }>()

  if (!urlSection) {
    return <Navigate to={Paths.ADMIN_USERS} replace />
  }

  return (
    <div className="min-w-0">
      {urlSection === 'users' && <UserManagementAdmin />}
      {urlSection === 'maintenance' && <MaintenanceAdmin />}
    </div>
  )
}
