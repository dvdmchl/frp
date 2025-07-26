import React, { useState } from "react";
import { UserManagementService } from "../../api/services/UserManagementService";
import type { UserRegisterRequestDto } from "../../api/models/UserRegisterRequestDto";
import type { UserDto } from "../../api/models/UserDto";
import { Form} from "../UIComponent/Form.tsx";
import { H2Title, TextError} from "../UIComponent/Text.tsx";
import { InputText, InputEmail, InputPassword } from "../UIComponent/Input.tsx";
import {RegisterButton} from "../UIComponent/Button.tsx";
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
        } catch (err: unknown) {
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
        <Form onSubmit={handleSubmit}>
            <H2Title>{t("register.title")}</H2Title>
            <InputText
                id = "fullName"
                name="fullName"
                placeholderTranslationKey= "register.fullName"
                labelTranslationKey="register.fullName"
                value={fullName}
                required
                onChange={e => setFullName(e.target.value)}
            />
            <InputEmail
                value={email}
                required
                onChange={e => setEmail(e.target.value)}
            />
            <InputPassword
                value={password}
                required
                onChange={e => setPassword(e.target.value)}
            />
            {error && <TextError message={error} />}
            <RegisterButton loading={loading} type="submit"/>
        </Form>
    );
};
