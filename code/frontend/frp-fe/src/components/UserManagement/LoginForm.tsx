import React, { useState } from 'react'
import { UserManagementService } from '../../api/services/UserManagementService'
import type { UserLoginResponseDto } from '../../api/models/UserLoginResponseDto'
import { Form } from '../UIComponent/Form.tsx'
import { H1Title } from '../UIComponent/Text.tsx'
import { InputEmail, InputPassword } from '../UIComponent/Input.tsx'
import { Button } from 'flowbite-react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../../api/core/ApiError'
import { ErrorDisplay } from '../UIComponent/ErrorDisplay.tsx'
import type { ErrorDto } from '../../api/models/ErrorDto' // Import ErrorDto

type LoginFormProps = {
  onLoginSuccess: (user: UserLoginResponseDto) => void
  onRegisterClick: () => void
}

export const LoginForm: React.FC<LoginFormProps> = ({ onLoginSuccess, onRegisterClick }) => {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState<ErrorDto | null>(null) // Use ErrorDto

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setApiError(null)
    try {
      const response = await UserManagementService.login({ email, password })
      if (response.token) {
        localStorage.setItem('jwt', response.token)
      }
      onLoginSuccess(response)
    } catch (err) {
      console.error('Login failed', err)
      if (err instanceof ApiError && err.errorDto) {
        setApiError(err.errorDto)
      } else {
        setApiError({ type: 'ClientError', message: t('login.error'), stackTrace: undefined })
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Form onSubmit={handleSubmit}>
      <H1Title>{t('login.title')}</H1Title>

      <InputEmail value={email} onChange={(e) => setEmail(e.target.value)} required />
      <InputPassword value={password} onChange={(e) => setPassword(e.target.value)} required />

      {apiError && <ErrorDisplay error={apiError} />}

      <Button disabled={loading} type="submit">
        {loading ? t('login.button-progress') : t('login.button')}
      </Button>
      <Button color="light" onClick={onRegisterClick} className="w-full">
        {t('register.title')}
      </Button>
    </Form>
  )
}
