import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
  Button,
  Spinner,
  Checkbox,
} from 'flowbite-react'
import { MaintenanceAdminService, ApiError } from '../../api'
import { ErrorDisplay } from '../UIComponent/ErrorDisplay'
import { H1Title, Paragraph, TextSuccess } from '../UIComponent/Text'

export function MaintenanceAdmin() {
  const { t } = useTranslation()
  const [orphanSchemas, setOrphanSchemas] = useState<string[]>([])
  const [selectedSchemas, setSelectedUserSchemas] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const fetchOrphanSchemas = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await MaintenanceAdminService.listOrphanSchemas()
      setOrphanSchemas(data)
      setSelectedUserSchemas([])
    } catch (err) {
      setError(err as ApiError)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchOrphanSchemas()
  }, [fetchOrphanSchemas])

  const handleCheckboxChange = (schemaName: string) => {
    setSelectedUserSchemas((prev) =>
      prev.includes(schemaName) ? prev.filter((s) => s !== schemaName) : [...prev, schemaName]
    )
  }

  const handleDropSelected = async () => {
    if (selectedSchemas.length === 0) return
    if (!confirm(t('admin.maintenance.dropConfirm'))) return

    setActionLoading(true)
    setError(null)
    setSuccess(null)
    try {
      await MaintenanceAdminService.dropOrphanSchemas(selectedSchemas)
      setSuccess(t('admin.maintenance.dropSuccess'))
      await fetchOrphanSchemas()
    } catch (err) {
      setError(err as ApiError)
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className="p-4 space-y-6">
      <H1Title>{t('admin.maintenance.title')}</H1Title>

      <ErrorDisplay error={error} />
      {success && <TextSuccess message={success} />}

      <div className="space-y-4">
        <div className="flex justify-between items-center">
          <h2 className="text-2xl font-bold">{t('admin.maintenance.orphanSchemas')}</h2>
          <Button
            color="failure"
            onClick={handleDropSelected}
            disabled={selectedSchemas.length === 0 || actionLoading}
          >
            {actionLoading ? <Spinner size="sm" className="mr-2" /> : null}
            {t('admin.maintenance.dropSelected')}
          </Button>
        </div>

        <Paragraph>{t('admin.maintenance.orphanSchemasDescription')}</Paragraph>

        {loading ? (
          <div className="flex justify-center p-10">
            <Spinner size="xl" />
          </div>
        ) : orphanSchemas.length === 0 ? (
          <div className="text-center p-10 text-gray-500">{t('admin.maintenance.noOrphanSchemas')}</div>
        ) : (
          <div className="overflow-x-auto">
            <Table hoverable>
              <TableHead>
                <TableRow>
                  <TableHeadCell className="p-4">
                    <Checkbox
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedUserSchemas([...orphanSchemas])
                        } else {
                          setSelectedUserSchemas([])
                        }
                      }}
                      checked={selectedSchemas.length === orphanSchemas.length && orphanSchemas.length > 0}
                    />
                  </TableHeadCell>
                  <TableHeadCell>{t('schema.name')}</TableHeadCell>
                </TableRow>
              </TableHead>
              <TableBody className="divide-y">
                {orphanSchemas.map((schema) => (
                  <TableRow key={schema} className="bg-white dark:border-gray-700 dark:bg-gray-800">
                    <TableCell className="p-4">
                      <Checkbox
                        checked={selectedSchemas.includes(schema)}
                        onChange={() => handleCheckboxChange(schema)}
                      />
                    </TableCell>
                    <TableCell className="whitespace-nowrap font-medium text-gray-900 dark:text-white">
                      {schema}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </div>
  )
}
