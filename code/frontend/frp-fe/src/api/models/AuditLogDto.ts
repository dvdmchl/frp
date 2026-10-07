/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AuditLogDto = {
    id?: number;
    createdAt?: string;
    userId?: number;
    userEmail?: string;
    action?: 'LOGIN' | 'LOGIN_FAILED' | 'LOGOUT' | 'USER_REGISTERED' | 'USER_INFO_CHANGED' | 'PASSWORD_CHANGED' | 'USER_ACTIVATED' | 'USER_DEACTIVATED' | 'ADMIN_GRANTED' | 'ADMIN_REVOKED' | 'USER_GROUPS_CHANGED' | 'SCHEMA_CREATED' | 'SCHEMA_COPIED' | 'SCHEMA_DROPPED' | 'CONNECTION_CREDENTIALS_CHANGED' | 'CONNECTION_DELETED';
    resource?: string;
    details?: string;
};

