/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AuditLogPageDto } from '../models/AuditLogPageDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AuditLogAdminService {
    /**
     * Search audit log
     * Returns audit log entries, newest first, optionally filtered by action, user and time range from (inclusive) to (exclusive).
     * @param action
     * @param userId
     * @param from
     * @param to
     * @param page
     * @param size
     * @returns AuditLogPageDto OK
     * @throws ApiError
     */
    public static searchAuditLog(
        action?: 'LOGIN' | 'LOGIN_FAILED' | 'LOGOUT' | 'USER_REGISTERED' | 'USER_INFO_CHANGED' | 'PASSWORD_CHANGED' | 'USER_ACTIVATED' | 'USER_DEACTIVATED' | 'ADMIN_GRANTED' | 'ADMIN_REVOKED' | 'USER_GROUPS_CHANGED' | 'SCHEMA_CREATED' | 'SCHEMA_COPIED' | 'SCHEMA_DROPPED' | 'CONNECTION_CREDENTIALS_CHANGED' | 'CONNECTION_DELETED',
        userId?: number,
        from?: string,
        to?: string,
        page?: number,
        size: number = 50,
    ): CancelablePromise<AuditLogPageDto> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/admin/audit-log',
            query: {
                'action': action,
                'userId': userId,
                'from': from,
                'to': to,
                'page': page,
                'size': size,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
}
