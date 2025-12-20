import React, { useState } from 'react'
import { UserManagementService } from '../../api/services/UserManagementService'
import type { UserRegisterRequestDto } from '../../api/models/UserRegisterRequestDto'
import type { UserDto } from '../../api/models/UserDto'
import { Form } from '../UIComponent/Form.tsx'
import { H2Title } from '../UIComponent/Text.tsx'
import { InputText, InputEmail, InputPassword } from '../UIComponent/Input.tsx'
import { Button } from 'flowbite-react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../../api/core/ApiError'
import { ErrorDisplay } from '../UIComponent/ErrorDisplay.tsx'
import type { ErrorDto } from '../../api/models/ErrorDto'

export const RegisterForm: React.FC<{ onRegisterSuccess: (user: UserDto) => void }> = ({ onRegisterSuccess }) => {
  const { t } = useTranslation()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState<ErrorDto | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setApiError(null)
    try {
      const data: UserRegisterRequestDto = { fullName, email, password }
      const response = await UserManagementService.register(data)
      if (response) {
        onRegisterSuccess(response)
      } else {
        setApiError({ type: 'ClientError', message: t('register.error'), stackTrace: undefined })
      }
    } catch (err: unknown) {
      console.error('Registration failed', err)
      if (err instanceof ApiError && err.errorDto) {
        setApiError(err.errorDto)
      } else {
        setApiError({ type: 'ClientError', message: t('register.error-unknown'), stackTrace: undefined })
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Form onSubmit={handleSubmit}>
      <H2Title>{t('register.title')}</H2Title>
      <InputText
        id="fullName"
        name="fullName"
        placeholderTranslationKey="register.fullName"
        labelTranslationKey="register.fullName"
        value={fullName}
        required
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFullName(e.target.value)}
      />
      <InputEmail value={email} required onChange={(e) => setEmail(e.target.value)} />
      <InputPassword value={password} required onChange={(e) => setPassword(e.target.value)} />
      {apiError && <ErrorDisplay error={apiError} />}
      <Button type="submit" color="green" disabled={loading}>
        {loading ? t('register.button-progress') : t('register.button')}
      </Button>
    </Form>
  )
}
