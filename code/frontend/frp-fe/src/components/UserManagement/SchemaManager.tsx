import React, {useCallback, useEffect, useRef, useState} from "react";
import type {MutableRefObject} from "react";
import {useTranslation} from "react-i18next";
import {SchemaManagementService} from "../../api/services/SchemaManagementService";
import {UserManagementService} from "../../api/services/UserManagementService";
import {
    Button, 
    Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow,
    Modal, ModalBody, ModalFooter, ModalHeader,
    Select, Label
} from "flowbite-react";
import {H2Title, TextSuccess} from "../UIComponent/Text";
import {ApiError} from "../../api/core/ApiError";
import {ErrorDisplay} from "../UIComponent/ErrorDisplay";
import type {ErrorDto} from "../../api/models/ErrorDto";
import {InputText} from "../UIComponent/Input";
import type {UserDto} from "../../api/models/UserDto";

interface SchemaManagerProps {
    user: UserDto | null;
    onUserUpdate: (user: UserDto) => void;
}

export const SchemaManager: React.FC<SchemaManagerProps> = ({user, onUserUpdate}) => {
    const {t} = useTranslation();
    const [schemas, setSchemas] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);
    const [apiError, setApiError] = useState<ErrorDto | null>(null);
    const [createError, setCreateError] = useState<ErrorDto | null>(null);
    const [copyError, setCopyError] = useState<ErrorDto | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [newSchemaName, setNewSchemaName] = useState("");
    const [copySource, setCopySource] = useState("");
    const [copyTarget, setCopyTarget] = useState("");
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showCopyModal, setShowCopyModal] = useState(false);
    const copyTargetInputRef: MutableRefObject<HTMLInputElement | null> = useRef(null);

    const loadSchemas = useCallback(async () => {
        try {
            const list = await SchemaManagementService.listMySchemas();
            setSchemas(list);
        } catch (err: unknown) {
            console.error("Failed to load schemas", err);
            if (err instanceof ApiError && err.errorDto) {
                setApiError(err.errorDto);
            } else {
                setApiError({type: "ClientError", message: t("schema.error"), stackTrace: undefined});
            }
        }
    }, [t]);

    useEffect(() => {
        loadSchemas();
    }, [loadSchemas]);

    useEffect(() => {
        if (showCopyModal) {
            const timer = setTimeout(() => {
                if (copyTargetInputRef.current) {
                    copyTargetInputRef.current.focus();
                }
            }, 100);
            return () => clearTimeout(timer);
        }
    }, [showCopyModal]);

    const handleCreate = async () => {
        setLoading(true);
        setCreateError(null);
        setSuccess(null);
        try {
            await SchemaManagementService.createSchema({name: newSchemaName}, false);
            setSuccess(t("schema.createSuccess"));
            setNewSchemaName("");
            setShowCreateModal(false);
            loadSchemas();
        } catch (err: unknown) {
            console.error("Create failed", err);
            if (err instanceof ApiError && err.errorDto) {
                setCreateError(err.errorDto);
            } else {
                setCreateError({type: "ClientError", message: t("schema.error"), stackTrace: undefined});
            }
        } finally {
            setLoading(false);
        }
    };

    const handleCopy = async () => {
        setLoading(true);
        setCopyError(null);
        setSuccess(null);
        try {
            await SchemaManagementService.copySchema({source: copySource, target: copyTarget});
            setSuccess(t("schema.copySuccess"));
            setCopySource("");
            setCopyTarget("");
            setShowCopyModal(false);
            loadSchemas();
        } catch (err: unknown) {
            console.error("Copy failed", err);
            if (err instanceof ApiError && err.errorDto) {
                setCopyError(err.errorDto);
            } else {
                setCopyError({type: "ClientError", message: t("schema.error"), stackTrace: undefined});
            }
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (name: string) => {
        if (!confirm(t("schema.deleteConfirm") || "Are you sure?")) return;
        setLoading(true);
        setApiError(null);
        setSuccess(null);
        try {
            await SchemaManagementService.deleteSchema(name);
            setSuccess(t("schema.deleteSuccess"));
            loadSchemas();
             // Refresh user if we deleted active schema (though backend might handle it or forbid it)
             const updatedUser = await UserManagementService.authenticatedUser();
             onUserUpdate(updatedUser);
        } catch (err: unknown) {
            console.error("Delete failed", err);
            if (err instanceof ApiError && err.errorDto) {
                setApiError(err.errorDto);
            } else {
                setApiError({type: "ClientError", message: t("schema.error"), stackTrace: undefined});
            }
        } finally {
            setLoading(false);
        }
    };

    const handleSwitch = async (name: string) => {
        setLoading(true);
        setApiError(null);
        setSuccess(null);
        try {
            await SchemaManagementService.setActiveSchema({name});
            setSuccess(t("schema.switchSuccess"));
            // Update user context with new active schema
            const updatedUser = await UserManagementService.authenticatedUser();
            onUserUpdate(updatedUser);
        } catch (err: unknown) {
            console.error("Switch failed", err);
            if (err instanceof ApiError && err.errorDto) {
                setApiError(err.errorDto);
            } else {
                setApiError({type: "ClientError", message: t("schema.error"), stackTrace: undefined});
            }
        } finally {
            setLoading(false);
        }
    };

    const handleOpenCopyModal = (schema: string) => {
        setCopySource(schema);
        setShowCopyModal(true);
    };

    return (
        <div className="flex flex-col gap-4">
            <H2Title>{t('schema.listTitle')}</H2Title>

            <div className="flex gap-2 mb-4">
                <Button onClick={() => setShowCreateModal(true)}>{t('schema.createTitle')}</Button>
            </div>

            {apiError && <ErrorDisplay error={apiError}/>}
            {success && <TextSuccess message={success}/>}

            <Table>
                <TableHead>
                    <TableHeadCell>{t('schema.name')}</TableHeadCell>
                    <TableHeadCell>{t('schema.active')}</TableHeadCell>
                    <TableHeadCell>Actions</TableHeadCell>
                </TableHead>
                <TableBody className="divide-y">
                    {schemas.map(schema => (
                        <TableRow key={schema} className="bg-white dark:border-gray-700 dark:bg-gray-800">
                            <TableCell className="whitespace-nowrap font-medium text-gray-900 dark:text-white">
                                {schema}
                            </TableCell>
                            <TableCell>
                                {user?.activeSchema === schema ? "✓" : ""}
                            </TableCell>
                            <TableCell>
                                <div className="flex gap-2">
                                    {user?.activeSchema !== schema && (
                                        <Button size="xs" onClick={() => handleSwitch(schema)}>
                                            {t('schema.switch')}
                                        </Button>
                                    )}
                                    <Button size="xs" color="light" onClick={() => handleOpenCopyModal(schema)}>
                                        {t('schema.copy')}
                                    </Button>
                                    <Button size="xs" color="failure" onClick={() => handleDelete(schema)}>
                                        {t('schema.delete')}
                                    </Button>
                                </div>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>

            <Modal show={showCreateModal} onClose={() => setShowCreateModal(false)}>
                <ModalHeader>{t('schema.createTitle')}</ModalHeader>
                <ModalBody>
                    <div className="space-y-6">
                         <InputText
                            id="newSchemaName"
                            name="newSchemaName"
                            placeholderTranslationKey="schema.name"
                            labelTranslationKey="schema.name"
                            value={newSchemaName}
                            onChange={e => setNewSchemaName(e.target.value)}
                        />
                        {createError && <ErrorDisplay error={createError} />}
                    </div>
                </ModalBody>
                <ModalFooter>
                    <Button onClick={handleCreate} disabled={loading || !newSchemaName}>{t('schema.create')}</Button>
                    <Button color="gray" onClick={() => setShowCreateModal(false)}>Cancel</Button>
                </ModalFooter>
            </Modal>

            <Modal show={showCopyModal} onClose={() => setShowCopyModal(false)}>
                <ModalHeader>{t('schema.copyTitle')}</ModalHeader>
                <ModalBody>
                    <div className="space-y-6">
                        <div>
                             <Label className="block mb-2 text-sm font-medium text-gray-900 dark:text-white">{t('schema.source')}</Label>
                             <Select 
                                value={copySource} 
                                onChange={e => setCopySource(e.target.value)}
                             >
                                <option value="">Select schema</option>
                                {schemas.map(s => <option key={s} value={s}>{s}</option>)}
                             </Select>
                        </div>
                        <InputText
                            id="copyTarget"
                            name="copyTarget"
                            placeholderTranslationKey="schema.target"
                            labelTranslationKey="schema.target"
                            value={copyTarget}
                            onChange={e => setCopyTarget(e.target.value)}
                            ref={copyTargetInputRef}
                        />
                        {copyError && <ErrorDisplay error={copyError} />}
                    </div>
                </ModalBody>
                <ModalFooter>
                    <Button onClick={handleCopy} disabled={loading || !copySource || !copyTarget}>{t('schema.copy')}</Button>
                    <Button color="gray" onClick={() => setShowCopyModal(false)}>Cancel</Button>
                </ModalFooter>
            </Modal>
        </div>
    );
};