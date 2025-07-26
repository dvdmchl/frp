import React from "react";
import {useTranslation} from "react-i18next";

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
    children: React.ReactNode;
    variant?: "primary" | "secondary";
};

export function Button({children, variant = "primary", disabled, className = "", ...props}: ButtonProps) {
    const baseClasses = "flex-1 font-semibold rounded-lg py-3 shadow-md transition";
    const variantClasses =
        variant === "primary"
            ? "bg-gradient-to-r from-blue-600 to-blue-500 text-white hover:from-blue-700 hover:to-blue-600 disabled:opacity-60"
            : "bg-gradient-to-r from-green-500 to-green-400 text-white hover:from-green-600 hover:to-green-500";

    return (
        <button
            disabled={disabled}
            className={`${baseClasses} ${variantClasses} ${className}`}
            {...props}
        >
            {children}
        </button>
    );
}

export function LoginButton({loading, ...props}: { loading: boolean } & Omit<ButtonProps, "children">) {
    const {t} = useTranslation();
    return (
        <Button {...props} variant="primary" disabled={loading}>
            {loading ? t("login.button-progress") : t("login.button")}
        </Button>
    );
}

export function RegisterButton({loading, ...props}: { loading: boolean } & Omit<ButtonProps, "children">) {
    const {t} = useTranslation();
    return (
        <Button {...props} variant="secondary" disabled={loading}>
            {loading ? t("register.button-progress") : t("register.button")}
        </Button>
    );
}

export function LogoutButton({loading, ...props}: { loading: boolean } & Omit<ButtonProps, "children">) {
    const {t} = useTranslation();
    return (
        <Button {...props} variant="secondary" disabled={loading}>
            {loading ? t("logout.button-progress") : t("logout.button")}
        </Button>
    );
}
