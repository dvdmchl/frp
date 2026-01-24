import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ModuleManagementService } from '../../api/services/ModuleManagementService'
import type { ModuleDefinitionDto } from '../../api/models/ModuleDefinitionDto'
import { ErrorDisplay } from '../UIComponent/ErrorDisplay'
import { H2Title, Paragraph } from '../UIComponent/Text'
import { Spinner } from 'flowbite-react'
import type { ErrorDto } from '../../api/models/ErrorDto'
import { AccountingModule } from './Accounting/AccountingModule'

export const ModuleWrapper: React.FC = () => {
  const { moduleCode } = useParams<{ moduleCode: string }>()
  const { t } = useTranslation()
  const [module, setModule] = useState<ModuleDefinitionDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ErrorDto | null>(null)

  useEffect(() => {
    const loadModule = async () => {
      if (!moduleCode) return

      setLoading(true)
      setError(null)
      try {
        const data = await ModuleManagementService.getModule(moduleCode)
        setModule(data)
      } catch (err: unknown) {
        // Assuming err handles structured error response or simple string
        const apiError = err as { body?: ErrorDto; status?: number }
        setError({
          message: apiError.body?.message || t('error.unknown'),
          stackTrace: apiError.body?.stackTrace,
        })
      } finally {
        setLoading(false)
      }
    }

    loadModule()
  }, [moduleCode, t])

  if (loading) {
    return (
      <div className="flex justify-center p-4">
        <Spinner size="xl" />
      </div>
    )
  }

  if (error) {
    return <ErrorDisplay error={error} />
  }

  if (!module) {
    return <ErrorDisplay error={{ message: t('module.notFound') }} />
  }

  if (module.state === 'DISABLED') {
    return (
      <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-md">
        <H2Title>{t('module.disabled.title')}</H2Title>
        <Paragraph>{t('module.disabled.message', { module: module.title })}</Paragraph>
      </div>
    )
  }

  if (module.code === 'ACC') {
    return <AccountingModule />
  }

  return (
    <div className="p-4">
      <H2Title>{module.title}</H2Title>
      <Paragraph>{module.description}</Paragraph>
      <div className="mt-4 p-4 border rounded-md bg-gray-50">
        <Paragraph>{t('module.placeholderContent')}</Paragraph>
      </div>
    </div>
  )
}
