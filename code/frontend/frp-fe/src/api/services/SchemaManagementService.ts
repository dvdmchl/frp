/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CopySchemaRequest } from '../models/CopySchemaRequest';
import type { CreateSchemaRequest } from '../models/CreateSchemaRequest';
import type { SetActiveSchemaRequest } from '../models/SetActiveSchemaRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class SchemaManagementService {
    /**
     * Set active schema
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public static setActiveSchema(
        requestBody: SetActiveSchemaRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/schema/active',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * List my schemas
     * @returns string OK
     * @throws ApiError
     */
    public static listMySchemas(): CancelablePromise<Array<string>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/schema',
        });
    }
    /**
     * Create new schema
     * @param requestBody
     * @param setActive
     * @returns string OK
     * @throws ApiError
     */
    public static createSchema(
        requestBody: CreateSchemaRequest,
        setActive: boolean = false,
    ): CancelablePromise<Record<string, string>> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/schema',
            query: {
                'setActive': setActive,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Copy schema
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public static copySchema(
        requestBody: CopySchemaRequest,
    ): CancelablePromise<Record<string, string>> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/schema/copy',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Delete schema
     * @param name
     * @returns any OK
     * @throws ApiError
     */
    public static deleteSchema(
        name: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/schema/{name}',
            path: {
                'name': name,
            },
        });
    }
}
