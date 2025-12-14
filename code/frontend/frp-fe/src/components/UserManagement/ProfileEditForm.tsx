import React, {useEffect, useState} from "react";
import {UserManagementService} from "../../api/services/UserManagementService";
import type {UserDto} from "../../api/models/UserDto";
import {Form} from "../UIComponent/Form.tsx";
import {H2Title, TextSuccess} from "../UIComponent/Text.tsx";
import {ApiError} from "../../api/core/ApiError";
import {ErrorDisplay} from "../UIComponent/ErrorDisplay.tsx";
import type {ErrorDto} from "../../api/models/ErrorDto";
import {
    InputText,
    InputEmail,
    InputNewPassword,
    InputOldPassword,
    InputConfirmPassword
} from "../UIComponent/Input.tsx";
import {Button, SidebarItemGroup, Sidebar, SidebarItem, SidebarItems} from "flowbite-react";
import {useTranslation} from "react-i18next";
import {SchemaManager} from "./SchemaManager";

export const ProfileEditForm: React.FC<{ onProfileUpdate?: (user: UserDto) => void }> = ({onProfileUpdate}) => {
    const {t} = useTranslation();
    const [section, setSection] = useState<'info' | 'security' | 'schemas'>('info');
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [apiError, setApiError] = useState<ErrorDto | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [userDto, setUserDto] = useState<UserDto | null>(null);

    useEffect(() => {
        UserManagementService.authenticatedUser()
            .then(user => {
                setUserDto(user);
                if (user.fullName) setFullName(user.fullName);
                if (user.email) setEmail(user.email);
            })
            .catch((err: unknown) => {
                console.error("Failed to load user", err);
                if (err instanceof ApiError && err.errorDto) {
                    setApiError(err.errorDto);
                } else {
                    setApiError({type: "ClientError", message: t("profile.error"), stackTrace: undefined});
                }
            });
    }, [t]);

    const handleInfoSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setApiError(null);
        setSuccess(null);
        try {
            const user = await UserManagementService.updateAuthenticatedUserInfo({fullName, email});
            if (onProfileUpdate) {
                onProfileUpdate(user);
            }
            setUserDto(user);
            setSuccess(t("profile.infoSuccess"));
        } catch (err: unknown) {
            console.error("Profile update failed", err);
            if (err instanceof ApiError && err.errorDto) {
                setApiError(err.errorDto);
            } else {
                setApiError({type: "ClientError", message: t("profile.infoError"), stackTrace: undefined});
            }
        } finally {
            setLoading(false);
        }
    };
    
    const handleUserUpdate = (updatedUser: UserDto) => {
        setUserDto(updatedUser);
        if (onProfileUpdate) {
            onProfileUpdate(updatedUser);
        }
    };

    const handlePasswordSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setApiError(null);
        setSuccess(null);
        if (newPassword !== confirmPassword) {
            setApiError({type: "ClientError", message: t("profile.passwordError"), stackTrace: undefined});
            setLoading(false);
            return;
        }
        try {
            await UserManagementService.changeAuthenticatedUserPassword({oldPassword, newPassword});
            setSuccess(t("profile.passwordSuccess"));
            setOldPassword("");
            setNewPassword("");
            setConfirmPassword("");
        } catch (err: unknown) {
            console.error("Password change failed", err);
            if (err instanceof ApiError && err.errorDto) {
                setApiError(err.errorDto);
            } else {
                setApiError({type: "ClientError", message: t("profile.passwordError"), stackTrace: undefined});
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex gap-6">
            <Sidebar aria-label="User Profile Sidebar">
                <SidebarItems>
                    <SidebarItemGroup>
                        <SidebarItem
                            active={section === 'info'}
                            onClick={() => setSection('info')}
                        >
                            {t('profile.personalInfo')}
                        </SidebarItem>
                        <SidebarItem
                            active={section === 'security'}
                            onClick={() => setSection('security')}
                        >
                            {t('profile.security')}
                        </SidebarItem>
                        <SidebarItem
                            active={section === 'schemas'}
                            onClick={() => setSection('schemas')}
                        >
                            {t('profile.databaseSchemas')}
                        </SidebarItem>
                    </SidebarItemGroup>
                </SidebarItems>
            </Sidebar>
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

                        {apiError && <ErrorDisplay error={apiError} />}
                        {success && <TextSuccess message={success}/>}

                        <Button disabled={loading} type="submit">
                            {loading ? t("profile.button-progress") : t("profile.button")}
                        </Button>
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

                        {apiError && <ErrorDisplay error={apiError} />}
                        {success && <TextSuccess message={success}/>}

                        <Button disabled={loading} type="submit">
                            {loading ? t("profile.button-progress") : t("profile.button")}
                        </Button>
                    </Form>
                )}

                {section === 'schemas' && (
                     <div className="max-w-4xl mx-auto mt-12 p-8 bg-bgForm rounded-2xl shadow-2xl">
                         <SchemaManager user={userDto} onUserUpdate={handleUserUpdate} />
                     </div>
                )}
            </div>
        </div>
    );
};