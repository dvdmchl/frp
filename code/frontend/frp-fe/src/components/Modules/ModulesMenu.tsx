import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Dropdown, DropdownItem } from 'flowbite-react'
import { ModuleManagementService } from '../../api/services/ModuleManagementService'
import type { ModuleDefinitionDto } from '../../api/models/ModuleDefinitionDto'
import { Paths } from '../../constants/Paths'

export const ModulesMenu: React.FC = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [modules, setModules] = useState<ModuleDefinitionDto[]>([])

  useEffect(() => {
    const loadModules = async () => {
      try {
        const data = await ModuleManagementService.listModules()
        setModules(data.filter((m) => m.state === 'ENABLED'))
      } catch (err) {
        console.error('Failed to load modules', err)
      }
    }
    loadModules()
  }, [])

  if (modules.length === 0) {
    return null
  }

  return (
    <Dropdown label={t('modules.menuTitle')} inline>
      {modules.map((module) => (
        <DropdownItem
          key={module.code}
          onClick={() => navigate(Paths.MODULES.replace(':moduleCode', module.code || ''))}
        >
          {module.title}
        </DropdownItem>
      ))}
    </Dropdown>
  )
}
