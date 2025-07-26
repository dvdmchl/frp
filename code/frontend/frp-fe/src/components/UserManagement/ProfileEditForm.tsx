import React, { useEffect, useState } from "react";
import { UserManagementService } from "../../api/services/UserManagementService";
import type { UserDto } from "../../api/models/UserDto";
import { Form } from "../UIComponent/Form.tsx";
import { H2Title, TextError, TextSuccess } from "../UIComponent/Text.tsx";
import { InputText, InputEmail, InputNewPassword } from "../UIComponent/Input.tsx";
import { ProfileButton } from "../UIComponent/Button.tsx";
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
        <Form onSubmit={handleSubmit}>
            <H2Title>{t("profile.title")}</H2Title>

            <InputText
                id="fullName"
                name="fullName"
                placeholderTranslationKey="profile.fullName"
                labelTranslationKey="profile.fullName"
                value={fullName}
                required
                onChange={e => setFullName(e.target.value)}
            />

            <InputEmail
                value={email}
                required
                onChange={e => setEmail(e.target.value)}
            />

            <InputNewPassword
                value={password}
                onChange={e => setPassword(e.target.value)}
            />

            {error && <TextError message={error} />}
            {success && <TextSuccess message={success} />}

            <ProfileButton loading={loading} type="submit" />
        </Form>
    );
};
