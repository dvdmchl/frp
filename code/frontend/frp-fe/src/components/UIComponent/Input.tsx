import React from "react";
import {useTranslation} from "react-i18next";

type BaseInputProps = React.InputHTMLAttributes<HTMLInputElement> & {
    type: string;
    labelTranslationKey: string; // pro label
    placeholderTranslationKey: string; // pro placeholder
    name: string;
    id: string;
    autoComplete: string;
};

function BaseInput({
                       labelTranslationKey,
                       placeholderTranslationKey,
                       ...props
                   }: BaseInputProps) {
    const {t} = useTranslation();

    return (
        <div className="flex flex-col">
            <label
                htmlFor={props.id}
                className="font-semibold mb-2 text-textLight"
            >
                {t(labelTranslationKey)}
            </label>
            <input
                placeholder={t(placeholderTranslationKey)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                {...props}
            />
        </div>
    );
}

export function InputText(
    props: Readonly<Omit<
        BaseInputProps,
        | "type"
        | "autoComplete"
    >>
) {
    return (
        <BaseInput
            type="text"
            autoComplete=""
            {...props}
        />
    );
}

export function InputEmail(
    props: Readonly<Omit<
        BaseInputProps,
        | "type"
        | "labelTranslationKey"
        | "placeholderTranslationKey"
        | "name"
        | "id"
        | "autoComplete"
    >>
) {
    return (
        <BaseInput
            type="email"
            labelTranslationKey="login.email"
            placeholderTranslationKey="login.email"
            name="email"
            id="email"
            autoComplete="email"
            {...props}
        />
    );
}

export function InputPassword(
    props: Readonly<Omit<
        BaseInputProps,
        | "type"
        | "labelTranslationKey"
        | "placeholderTranslationKey"
        | "name"
        | "id"
        | "autoComplete"
    >>
) {
    return (
        <BaseInput
            type="password"
            labelTranslationKey="login.password"
            placeholderTranslationKey="login.password"
            name="password"
            id="password"
            autoComplete="current-password"
            {...props}
        />
    );
}

export function InputNewPassword(
    props: Readonly<Omit<
        BaseInputProps,
        | "type"
        | "labelTranslationKey"
        | "placeholderTranslationKey"
        | "name"
        | "id"
        | "autoComplete"
    >>
) {
    return (
        <BaseInput
            type="password"
            labelTranslationKey="profile.password"
            placeholderTranslationKey="profile.password"
            name="password"
            id="password"
            autoComplete="new-password"
            {...props}
        />
    );
}
