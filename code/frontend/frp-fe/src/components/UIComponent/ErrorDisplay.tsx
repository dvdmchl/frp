// code/frontend/frp-fe/src/components/UIComponent/ErrorDisplay.tsx
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ErrorDto } from '../../api/models/ErrorDto';
import { TextError } from './Text';
import { Button } from 'flowbite-react';

interface ErrorDisplayProps {
    error: ErrorDto | null;
    className?: string;
}

export const ErrorDisplay: React.FC<ErrorDisplayProps> = ({ error, className }) => {
    const { t } = useTranslation();
    const [showStackTrace, setShowStackTrace] = useState(false);

    if (!error || (!error.message && !error.stackTrace)) {
        return null;
    }

    const toggleStackTrace = () => {
        setShowStackTrace(!showStackTrace);
    };

    return (
        <div className={`p-4 rounded-md bg-red-100 border border-red-400 text-red-700 ${className}`}>
            <TextError message={error.message || t('error.unknown')}/>
            {error.stackTrace && (
                <div className="mt-2">
                    <Button size="xs" color="light" onClick={toggleStackTrace}>
                        {showStackTrace ? t('error.hideStackTrace') : t('error.showStackTrace')}
                    </Button>
                    {showStackTrace && (
                        <pre className="mt-2 p-2 bg-red-50 text-red-600 rounded-md overflow-auto text-xs">
                            {error.stackTrace}
                        </pre>
                    )}
                </div>
            )}
        </div>
    );
};
