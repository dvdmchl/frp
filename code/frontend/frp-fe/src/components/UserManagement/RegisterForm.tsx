import React, { useState } from "react";
import { UserManagementService } from "../../api/services/UserManagementService";
import type { UserRegisterRequestDto } from "../../api/models/UserRegisterRequestDto";
import type { UserDto } from "../../api/models/UserDto";
import {useTranslation} from "react-i18next";

export const RegisterForm: React.FC<{ onRegisterSuccess: (user: UserDto) => void }> = ({ onRegisterSuccess }) => {
    const {t} = useTranslation();
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            const data: UserRegisterRequestDto = { fullName, email, password };
            const response = await UserManagementService.register(data);
            if (response) {
                onRegisterSuccess(response);
            } else {
                setError(t("register.error"));
            }
        } catch (err: any) {
            // openapi-typescript-codegen hází ApiError, můžeš upřesnit detekci chyb:
            if (err?.status === 409) {
                setError( t("register.error-409"));
            } else if (err?.status === 400) {
                setError(t("register.error-400"));
            } else {
                setError(t("register.error-unknown"));
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="max-w-sm mx-auto mt-8 p-6 bg-white shadow-xl rounded-lg flex flex-col gap-4">
            <h2 className="text-xl font-semibold text-center">Registrace</h2>
            <input
                className="border rounded p-2"
                type="text"
                placeholder={t("register.fullName")}
                value={fullName}
                required
                onChange={e => setFullName(e.target.value)}
            />
            <input
                className="border rounded p-2"
                type="email"
                placeholder={t("register.email")}
                value={email}
                required
                onChange={e => setEmail(e.target.value)}
            />
            <input
                className="border rounded p-2"
                type="password"
                placeholder={t("register.password")}
                value={password}
                required
                onChange={e => setPassword(e.target.value)}
            />
            {error && <div className="text-red-600 text-sm">{error}</div>}
            <button type="submit" disabled={loading} className="bg-green-600 text-white rounded p-2 hover:bg-green-700">
                {loading ? t("register.button-progress"): t("register.button")}
            </button>
        </form>
    );
};
