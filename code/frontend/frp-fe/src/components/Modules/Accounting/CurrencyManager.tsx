import React, { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AccountingService } from '../../../api/services/AccountingService'
import type { AccCurrencyDto } from '../../../api/models/AccCurrencyDto'
import {
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Checkbox,
  Label,
  Spinner,
} from 'flowbite-react'
import { H2Title, TextSuccess } from '../../UIComponent/Text'
import { ApiError } from '../../../api/core/ApiError'
import { ErrorDisplay } from '../../UIComponent/ErrorDisplay'
import type { ErrorDto } from '../../../api/models/ErrorDto'
import { InputText, InputNumber } from '../../UIComponent/Input'

export const CurrencyManager: React.FC = () => {
  const { t } = useTranslation()
  const [currencies, setCurrencies] = useState<AccCurrencyDto[]>([])
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState<ErrorDto | null>(null)
  const [actionError, setActionError] = useState<ErrorDto | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [showModal, setShowModal] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [currentId, setCurrentId] = useState<number | null>(null)

  const [formData, setFormData] = useState({
    code: '',
    name: '',
    scale: 2,
    isBase: false,
  })

  const loadCurrencies = useCallback(async () => {
    setLoading(true)
    try {
      const list = await AccountingService.getAllCurrencies()
      setCurrencies(list)
    } catch (err: unknown) {
      handleError(err, setApiError)
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadCurrencies()
  }, [loadCurrencies])

  const handleError = (err: unknown, setError: React.Dispatch<React.SetStateAction<ErrorDto | null>>) => {
    console.error(err)
    if (err instanceof ApiError && err.errorDto) {
      setError(err.errorDto)
    } else {
      setError({ message: t('currency.error'), stackTrace: undefined })
    }
  }

  const handleOpenCreate = () => {
    setFormData({ code: '', name: '', scale: 2, isBase: false })
    setIsEdit(false)
    setCurrentId(null)
    setActionError(null)
    setSuccess(null)
    setShowModal(true)
  }

  const handleOpenEdit = (c: AccCurrencyDto) => {
    setFormData({
      code: c.code || '',
      name: c.name || '',
      scale: c.scale || 2,
      isBase: c.isBase || false,
    })
    setIsEdit(true)
    setCurrentId(c.id || null)
    setActionError(null)
    setSuccess(null)
    setShowModal(true)
  }

  const handleSubmit = async () => {
    setLoading(true)
    setActionError(null)
    setSuccess(null)
    try {
      if (isEdit && currentId) {
        await AccountingService.updateCurrency(currentId, {
          name: formData.name,
          scale: formData.scale,
        })
        setSuccess(t('currency.updateSuccess'))
      } else {
        await AccountingService.createCurrency({
          code: formData.code,
          name: formData.name,
          scale: formData.scale,
          isBase: formData.isBase,
        })
        setSuccess(t('currency.createSuccess'))
      }
      setShowModal(false)
      loadCurrencies()
    } catch (err: unknown) {
      handleError(err, setActionError)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm(t('currency.deleteConfirm'))) return
    setLoading(true)
    setActionError(null)
    setSuccess(null)
    try {
      await AccountingService.deleteCurrency(id)
      setSuccess(t('currency.deleteSuccess'))
      loadCurrencies()
    } catch (err: unknown) {
      handleError(err, setApiError) // Global error for delete
    } finally {
      setLoading(false)
    }
  }

  const handleSetBase = async (id: number) => {
    setLoading(true)
    setActionError(null)
    setSuccess(null)
    try {
      await AccountingService.setBaseCurrency(id)
      setSuccess(t('currency.setBaseSuccess'))
      loadCurrencies()
    } catch (err: unknown) {
      handleError(err, setApiError)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <H2Title>{t('currency.title')}</H2Title>

      <div className="flex gap-2 mb-4">
        <Button onClick={handleOpenCreate}>{t('currency.createTitle')}</Button>
      </div>

      {loading && currencies.length === 0 && (
          <div className="flex justify-center p-4">
            <Spinner size="xl" />
          </div>
      )}

      {apiError && <ErrorDisplay error={apiError} />}
      {success && <TextSuccess message={success} />}

      <Table>
        <TableHead>
          <TableRow>
            <TableHeadCell>{t('currency.code')}</TableHeadCell>
            <TableHeadCell>{t('currency.name')}</TableHeadCell>
            <TableHeadCell>{t('currency.scale')}</TableHeadCell>
            <TableHeadCell>{t('currency.isBase')}</TableHeadCell>
            <TableHeadCell>{t('common.actions')}</TableHeadCell>
          </TableRow>
        </TableHead>
        <TableBody className="divide-y">
          {currencies.map((c) => (
            <TableRow key={c.id} className="bg-white dark:border-gray-700 dark:bg-gray-800">
              <TableCell className="whitespace-nowrap font-medium text-gray-900 dark:text-white">{c.code}</TableCell>
              <TableCell>{c.name}</TableCell>
              <TableCell>{c.scale}</TableCell>
              <TableCell>{c.isBase ? '✓' : ''}</TableCell>
              <TableCell>
                <div className="flex gap-2">
                  <Button size="xs" color="light" onClick={() => handleOpenEdit(c)}>
                    {t('currency.update')}
                  </Button>
                  {!c.isBase && (
                    <Button size="xs" color="warning" onClick={() => c.id && handleSetBase(c.id)}>
                        {t('currency.setBase')}
                    </Button>
                  )}
                  <Button size="xs" color="failure" onClick={() => c.id && handleDelete(c.id)}>
                    {t('currency.delete')}
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Modal show={showModal} onClose={() => setShowModal(false)}>
        <ModalHeader>{isEdit ? t('currency.editTitle') : t('currency.createTitle')}</ModalHeader>
        <ModalBody>
          <div className="space-y-6">
            {!isEdit && (
                <InputText
                id="code"
                name="code"
                labelTranslationKey="currency.code"
                placeholderTranslationKey="currency.code"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                />
            )}
            <InputText
              id="name"
              name="name"
              labelTranslationKey="currency.name"
              placeholderTranslationKey="currency.name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
            <InputNumber
              id="scale"
              name="scale"
              labelTranslationKey="currency.scale"
              placeholderTranslationKey="currency.scale"
              value={formData.scale}
              onChange={(e) => setFormData({ ...formData, scale: parseInt(e.target.value) || 0 })}
            />
            {!isEdit && (
                <div className="flex items-center gap-2">
                    <Checkbox 
                        id="isBase" 
                        checked={formData.isBase} 
                        onChange={(e) => setFormData({...formData, isBase: e.target.checked})}
                    />
                    <Label htmlFor="isBase">{t('currency.isBase')}</Label>
                </div>
            )}
            {actionError && <ErrorDisplay error={actionError} />}
          </div>
        </ModalBody>
        <ModalFooter>
          <Button onClick={handleSubmit} disabled={loading || !formData.code || !formData.name}>
            {isEdit ? t('currency.update') : t('currency.create')}
          </Button>
          <Button color="gray" onClick={() => setShowModal(false)}>
            {t('common.close')}
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  )
}
