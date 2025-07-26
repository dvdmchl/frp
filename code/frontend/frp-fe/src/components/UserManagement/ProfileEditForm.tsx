import React, { useEffect, useState } from "react";
import { UserManagementService } from "../../api/services/UserManagementService";
import type { UserDto } from "../../api/models/UserDto";
import { Form } from "../UIComponent/Form.tsx";
import { H2Title, TextError, TextSuccess } from "../UIComponent/Text.tsx";
import { InputText, InputEmail, InputNewPassword, InputOldPassword, InputConfirmPassword } from "../UIComponent/Input.tsx";
import { ProfileButton } from "../UIComponent/Button.tsx";
import { SideMenu } from "../UIComponent/SideMenu.tsx";
import { useTranslation } from "react-i18next";

export const ProfileEditForm: React.FC<{ onProfileUpdate?: (user: UserDto) => void }> = ({ onProfileUpdate }) => {
    const { t } = useTranslation();
    const [section, setSection] = useState<'info' | 'security' | 'schemas'>('info');
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
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

    const handleInfoSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);
        try {
            const user = await UserManagementService.updateAuthenticatedUserInfo({ fullName, email });
            if (onProfileUpdate) {
                onProfileUpdate(user);
            }
            setSuccess(t("profile.infoSuccess"));
        } catch (err) {
            console.error("Profile update failed", err);
            setError(t("profile.infoError"));
        } finally {
            setLoading(false);
        }
    };

    const handlePasswordSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);
        if (newPassword !== confirmPassword) {
            setError(t("profile.passwordError"));
            setLoading(false);
            return;
        }
        try {
            await UserManagementService.changeAuthenticatedUserPassword({ oldPassword, newPassword });
            setSuccess(t("profile.passwordSuccess"));
            setOldPassword("");
            setNewPassword("");
            setConfirmPassword("");
        } catch (err) {
            console.error("Password change failed", err);
            setError(t("profile.passwordError"));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex gap-6">
            <SideMenu<'info' | 'security' | 'schemas'>
                items={[
                    { key: 'info', label: t('profile.personalInfo') },
                    { key: 'security', label: t('profile.security') },
                    { key: 'schemas', label: t('profile.databaseSchemas') },
                ]}
                selected={section}
                onSelect={setSection}
            />

            <div className="flex-1">
                {section === 'info' && (
                    <Form onSubmit={handleInfoSubmit}>
                        <H2Title>{t('profile.title')}</H2Title>

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

                        {error && <TextError message={error} />}
                        {success && <TextSuccess message={success} />}

                        <ProfileButton loading={loading} type="submit" />
                    </Form>
                )}

                {section === 'security' && (
                    <Form onSubmit={handlePasswordSubmit}>
                        <H2Title>{t('profile.security')}</H2Title>

                        <InputOldPassword
                            value={oldPassword}
                            required
                            onChange={e => setOldPassword(e.target.value)}
                        />

                        <InputNewPassword
                            value={newPassword}
                            onChange={e => setNewPassword(e.target.value)}
                        />

                        <InputConfirmPassword
                            value={confirmPassword}
                            onChange={e => setConfirmPassword(e.target.value)}
                        />

                        {error && <TextError message={error} />}
                        {success && <TextSuccess message={success} />}

                        <ProfileButton loading={loading} type="submit" />
                    </Form>
                )}

                {section === 'schemas' && (
                    <Form onSubmit={e => e.preventDefault()}>
                        <H2Title>{t('profile.databaseSchemas')}</H2Title>
                        <p className="text-textPrimary">Test</p>
                    </Form>
                )}
            </div>
        </div>
    );
};
