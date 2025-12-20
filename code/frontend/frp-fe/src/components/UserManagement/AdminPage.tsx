import React from 'react'
import { SidebarItemGroup, Sidebar, SidebarItem, SidebarItems } from 'flowbite-react'
import { useTranslation } from 'react-i18next'
import { Link, useParams, Navigate } from 'react-router-dom'
import { Paths } from '../../constants/Paths'
import { UserManagementAdmin } from './UserManagementAdmin'
import { MaintenanceAdmin } from './MaintenanceAdmin'

export const AdminPage: React.FC = () => {
  const { t } = useTranslation()
  const { section: urlSection } = useParams<{ section: string }>()

  const section = urlSection === 'maintenance' ? 'maintenance' : urlSection === 'users' ? 'users' : null

  if (!urlSection) {
    return <Navigate to={Paths.ADMIN_USERS} replace />
  }

  return (
    <div className="flex gap-6">
      <Sidebar aria-label="Admin Sidebar">
        <SidebarItems>
          <SidebarItemGroup>
            <SidebarItem
              active={section === 'users'}
              as={Link}
              // @ts-expect-error: flowbite-react polymorphic type issue
              to={Paths.ADMIN_USERS}
              className="cursor-pointer"
            >
              {t('admin.userManagement.title')}
            </SidebarItem>
            <SidebarItem
              active={section === 'maintenance'}
              as={Link}
              // @ts-expect-error: flowbite-react polymorphic type issue
              to={Paths.ADMIN_MAINTENANCE}
              className="cursor-pointer"
            >
              {t('admin.maintenance.title')}
            </SidebarItem>
          </SidebarItemGroup>
        </SidebarItems>
      </Sidebar>
      <div className="flex-1">
        {section === 'users' && <UserManagementAdmin />}
        {section === 'maintenance' && <MaintenanceAdmin />}
      </div>
    </div>
  )
}
