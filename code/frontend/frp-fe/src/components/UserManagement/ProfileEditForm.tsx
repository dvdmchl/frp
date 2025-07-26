import React, { useEffect, useState } from "react";
import { UserManagementService } from "../../api/services/UserManagementService";
import type { UserDto } from "../../api/models/UserDto";
import { useTranslation } from "react-i18next";

export const ProfileEditForm: React.FC<{ onProfileUpdate?: (user: UserDto) => void }> = ({ onProfileUpdate }) => {
    const { t } = useTranslation();
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    useEffect(() => {
        UserManagementService.authenticatedUser()
            .then(user => {
                if (user.fullName) setFullName(user.fullName);
                if (user.email) setEmail(user.email);
            })
            .catch(() => setError(t("profile.error")));
    }, [t]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);
        try {
            const user = await UserManagementService.updateAuthenticatedUser({ fullName, email, password });
            if (onProfileUpdate) {
                onProfileUpdate(user);
            }
            setSuccess(t("profile.success"));
            setPassword("");
        } catch (err) {
            console.error("Profile update failed", err);
            setError(t("profile.error"));
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="max-w-sm mx-auto mt-8 p-6 bg-white shadow-xl rounded-lg flex flex-col gap-4">
            <h2 className="text-xl font-semibold text-center">{t("profile.title")}</h2>
            <input
                className="border rounded p-2"
                type="text"
                placeholder={t("profile.fullName")}
                value={fullName}
                required
                onChange={e => setFullName(e.target.value)}
            />
            <input
                className="border rounded p-2"
                type="email"
                placeholder={t("profile.email")}
                value={email}
                required
                onChange={e => setEmail(e.target.value)}
            />
            <input
                className="border rounded p-2"
                type="password"
                placeholder={t("profile.password")}
                value={password}
                onChange={e => setPassword(e.target.value)}
            />
            {error && <div className="text-red-600 text-sm">{error}</div>}
            {success && <div className="text-green-600 text-sm">{success}</div>}
            <button type="submit" disabled={loading} className="bg-blue-600 text-white rounded p-2 hover:bg-blue-700">
                {loading ? t("profile.button-progress") : t("profile.button")}
            </button>
        </form>
    );
};
