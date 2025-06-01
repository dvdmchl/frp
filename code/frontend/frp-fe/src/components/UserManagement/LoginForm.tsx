import React, {useState} from "react";
import {UserManagementService} from "../../api/services/UserManagementService";
import type {UserLoginRequestDto} from "../../api/models/UserLoginRequestDto";
import type {UserLoginResponseDto} from "../../api/models/UserLoginResponseDto";
import {useTranslation} from "react-i18next";

export const LoginForm: React.FC<{ onLoginSuccess: (user: UserLoginResponseDto) => void }> = ({onLoginSuccess}) => {
    const {t} = useTranslation();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            const data: UserLoginRequestDto = {email, password};
            const response = await UserManagementService.login(data);
            onLoginSuccess(response); // response je už UserLoginResponseDto
        } catch (err: any) {
            setError(t("login.error"));
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit}
              className="max-w-sm mx-auto mt-8 p-6 bg-white shadow-xl rounded-lg flex flex-col gap-4">
            <h2 className="text-xl font-semibold text-center">{t("login.title")}</h2>
            <input
                className="border rounded p-2"
                type="email"
                placeholder={t("login.email")}
                value={email}
                required
                onChange={e => setEmail(e.target.value)}
            />
            <input
                className="border rounded p-2"
                type="password"
                placeholder={t("login.password")}
                value={password}
                required
                onChange={e => setPassword(e.target.value)}
            />
            {error && <div className="text-red-600 text-sm">{error}</div>}
            <button type="submit" disabled={loading} className="bg-blue-600 text-white rounded p-2 hover:bg-blue-700">
                {loading ? t("login.button-progress") : t("login.button")}
            </button>
        </form>
    );
};
