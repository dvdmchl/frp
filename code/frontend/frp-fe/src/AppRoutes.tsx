import { Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import { LoginForm } from './components/UserManagement/LoginForm'
import { RegisterForm } from './components/UserManagement/RegisterForm'
import { ProfileEditForm } from './components/UserManagement/ProfileEditForm'
import { UserManagementAdmin } from './components/UserManagement/UserManagementAdmin'
import { HomePage } from './components/HomePage'
import type { UserLoginResponseDto } from './api/models/UserLoginResponseDto'
import type { UserDto } from './api/models/UserDto'
import type { JSX } from 'react'
import { Paths } from './constants/Paths'

// Wrapper component for LoginForm with navigation to register page

type LoginPageProps = {
  onLoginSuccess: (user: UserLoginResponseDto) => void
}

type AppRoutesProps = {
  user: UserDto | null
  onLoginSuccess: (user: UserLoginResponseDto) => void
  onRegisterSuccess: () => void
  setUser: React.Dispatch<React.SetStateAction<UserDto | null>>
}

function LoginPage({ onLoginSuccess }: Readonly<LoginPageProps>): JSX.Element {
  const navigate = useNavigate()

  const handleRegisterClick = () => {
    navigate(Paths.REGISTER)
  }

  return <LoginForm onLoginSuccess={onLoginSuccess} onRegisterClick={handleRegisterClick} />
}

export function AppRoutes({ user, onLoginSuccess, onRegisterSuccess, setUser }: Readonly<AppRoutesProps>) {
  return (
    <Routes>
      {!user ? (
        <>
          <Route path={Paths.LOGIN} element={<LoginPage onLoginSuccess={onLoginSuccess} />} />
          <Route path={Paths.REGISTER} element={<RegisterForm onRegisterSuccess={onRegisterSuccess} />} />
          <Route path={Paths.WILDCARD} element={<Navigate to={Paths.LOGIN} />} />
        </>
      ) : (
        <>
          <Route path={Paths.HOME} element={<HomePage user={user} />} />
          <Route path={Paths.ADMIN_USERS} element={<UserManagementAdmin />} />
          <Route path={Paths.PROFILE} element={<Navigate to={Paths.PROFILE_PERSONAL_INFO} replace />} />
          <Route path={Paths.PROFILE_SECTION} element={<ProfileEditForm onProfileUpdate={setUser} />} />
          <Route path={Paths.WILDCARD} element={<Navigate to={Paths.HOME} />} />
        </>
      )}
    </Routes>
  )
}
