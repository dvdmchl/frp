import React, {useState} from "react";
import {UserManagementService} from "../../api/services/UserManagementService";
import type {UserLoginRequestDto} from "../../api/models/UserLoginRequestDto";
import type {UserLoginResponseDto} from "../../api/models/UserLoginResponseDto";
import { Button } from "flowbite-react";
import {InputEmail, InputPassword} from "../UIComponent/Input.tsx";
import {H2Title, TextError} from "../UIComponent/Text.tsx";
import {Form} from "../UIComponent/Form.tsx";
import {useTranslation} from "react-i18next";

export const LoginForm: React.FC<{
    onLoginSuccess: (user: UserLoginResponseDto) => void,
    onRegisterClick: () => void
}> = ({onLoginSuccess, onRegisterClick}) => {
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
            onLoginSuccess(response);
        } catch (err: unknown) {
            console.error("Login failed", err);
            setError(t("login.error"));
        } finally {
            setLoading(false);
        }
    };

    return (
        <Form onSubmit={handleSubmit}>
            <H2Title>{t("login.title")}</H2Title>

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

            {error && <TextError message={error}/>}

            <Button type="submit" disabled={loading}>
                {loading ? t("login.button-progress") : t("login.button")}
            </Button>
            <Button type="button" color="green" onClick={onRegisterClick} disabled={loading}>
                {loading ? t("register.button-progress") : t("register.button")}
            </Button>
        </Form>
    );
};