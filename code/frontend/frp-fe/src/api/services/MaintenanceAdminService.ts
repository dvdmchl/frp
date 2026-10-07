/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class MaintenanceAdminService {
    /**
     * List orphan schemas
     * Returns schemas that are not tracked in the frp_schema table.
     * @returns string OK
     * @throws ApiError
     */
    public static listOrphanSchemas(): CancelablePromise<Array<string>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/admin/maintenance/orphan-schemas',
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
    /**
     * Drop orphan schemas
     * Drops the specified orphan schemas.
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public static dropOrphanSchemas(
        requestBody: Array<string>,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/admin/maintenance/orphan-schemas',
            body: requestBody,
            mediaType: 'application/json',
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
