import React, { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, Label, Select, Textarea } from 'flowbite-react'
import type { AccAccountCreateRequestDto } from '../../../api/models/AccAccountCreateRequestDto'
import type { AccCurrencyDto } from '../../../api/models/AccCurrencyDto'
import { H3Title } from '../../UIComponent/Text'
import { InputText } from '../../UIComponent/Input'

interface AccountFormProps {
  initialData?: Partial<AccAccountCreateRequestDto> & { id?: number }
  currencies: AccCurrencyDto[]
  possibleParents: { id: number; name: string }[]
  onSubmit: (data: AccAccountCreateRequestDto) => Promise<void>
  onCancel: () => void
}

export const AccountForm: React.FC<AccountFormProps> = ({
  initialData,
  currencies,
  possibleParents,
  onSubmit,
  onCancel,
}) => {
  const { t } = useTranslation()
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { isSubmitting, errors },
  } = useForm<AccAccountCreateRequestDto>({
    defaultValues: {
      isPlaceholder: false,
      isLiquid: false,
      ...initialData,
    },
  })

  useEffect(() => {
    if (initialData) {
      reset({
        isPlaceholder: false,
        isLiquid: false,
        ...initialData,
      })
    }
  }, [initialData, reset])

  const isPlaceholder = watch('isPlaceholder')

  const submitHandler = async (data: AccAccountCreateRequestDto) => {
    // cleanup empty strings to undefined if necessary
    if (!data.parentId || data.parentId.toString() === '') {
      data.parentId = undefined
    }
    await onSubmit(data)
  }

  return (
    <form onSubmit={handleSubmit(submitHandler)} className="flex flex-col gap-4 p-4 bg-white rounded-lg shadow">
      <H3Title>{initialData?.id ? t('account.editTitle') : t('account.createTitle')}</H3Title>

      <InputText
        id="name"
        labelTranslationKey="account.name"
        placeholderTranslationKey="account.name"
        {...register('name', { required: true })}
      />
      {errors.name && <span className="text-red-500 text-sm">Required</span>}

      <div>
        <Label htmlFor="description">{t('account.description')}</Label>
        <Textarea id="description" placeholder={t('account.description')} rows={3} {...register('description')} />
      </div>

      <div>
        <div className="flex items-center gap-2">
          <Checkbox id="isPlaceholder" {...register('isPlaceholder')} />
          <Label htmlFor="isPlaceholder">{t('account.isPlaceholder')}</Label>
        </div>
      </div>

      {!isPlaceholder && (
        <>
          <div>
            <Label htmlFor="currencyCode">{t('account.currency')}</Label>
            <Select id="currencyCode" {...register('currencyCode', { required: !isPlaceholder })}>
              <option value="">{t('common.none')}</option>
              {currencies.map((c) => (
                <option key={c.id} value={c.code}>
                  {c.code} - {c.name}
                </option>
              ))}
            </Select>
            {errors.currencyCode && <span className="text-red-500 text-sm">Required</span>}
          </div>

          <div>
            <div className="flex items-center gap-2">
              <Checkbox id="isLiquid" {...register('isLiquid')} />
              <Label htmlFor="isLiquid">{t('account.isLiquid')}</Label>
            </div>
          </div>
        </>
      )}

      <div>
        <Label htmlFor="accountType">{t('account.type')}</Label>
        <Select id="accountType" {...register('accountType', { required: true })}>
          <option value="ASSET">{t('account.types.ASSET')}</option>
          <option value="LIABILITY">{t('account.types.LIABILITY')}</option>
          <option value="EQUITY">{t('account.types.EQUITY')}</option>
          <option value="REVENUE">{t('account.types.REVENUE')}</option>
          <option value="EXPENSE">{t('account.types.EXPENSE')}</option>
        </Select>
        {errors.accountType && <span className="text-red-500 text-sm">Required</span>}
      </div>

      <div>
        <Label htmlFor="parentId">{t('account.parent')}</Label>
        <Select id="parentId" {...register('parentId')}>
          <option value="">{t('common.none')}</option>
          {possibleParents.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </Select>
      </div>

      <div className="flex gap-2 justify-end mt-4">
        <Button color="gray" onClick={onCancel}>
          {t('common.close')}
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '...' : initialData?.id ? t('account.update') : t('account.create')}
        </Button>
      </div>
    </form>
  )
}
