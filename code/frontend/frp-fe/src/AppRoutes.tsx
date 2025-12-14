import {Routes, Route, Navigate, useNavigate} from "react-router-dom";
import { LoginForm } from "./components/UserManagement/LoginForm";
import { RegisterForm } from "./components/UserManagement/RegisterForm";
import { ProfileEditForm } from "./components/UserManagement/ProfileEditForm";
import { HomePage } from "./components/HomePage";
import type { UserLoginResponseDto } from "./api/models/UserLoginResponseDto";
import type { UserDto } from "./api/models/UserDto";
import type {JSX} from "react";

// Wrapper component for LoginForm with navigation to register page

type LoginPageProps = {
    onLoginSuccess: (user: UserLoginResponseDto) => void;
};

type AppRoutesProps = {
    user: UserDto | null;
    onLoginSuccess: (user: UserLoginResponseDto) => void;
    onRegisterSuccess: () => void;
    setUser: React.Dispatch<React.SetStateAction<UserDto | null>>;
};

function LoginPage({ onLoginSuccess }: Readonly<LoginPageProps>): JSX.Element {
    const navigate = useNavigate();

    const handleRegisterClick = () => {
        navigate("/register");
    };

    return <LoginForm onLoginSuccess={onLoginSuccess} onRegisterClick={handleRegisterClick} />;
}

export function AppRoutes({ user, onLoginSuccess, onRegisterSuccess, setUser } : Readonly<AppRoutesProps>) {
    return (
        <Routes>
            {!user ? (
                <>
                    <Route path="/login" element={<LoginPage onLoginSuccess={onLoginSuccess} />} />
                    <Route path="/register" element={<RegisterForm onRegisterSuccess={onRegisterSuccess} />} />
                    <Route path="*" element={<Navigate to="/login" />} />
                </>
            ) : (
                <>
                    <Route path="/" element={<HomePage user={user} />} />
                    <Route path="/profile" element={<Navigate to="/profile/personal-info" replace />} />
                    <Route path="/profile/:section" element={<ProfileEditForm onProfileUpdate={setUser} />} />
                    <Route path="*" element={<Navigate to="/" />} />
                </>
            )}
        </Routes>
    );
}
