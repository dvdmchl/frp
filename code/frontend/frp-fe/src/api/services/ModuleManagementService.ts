/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ModuleDefinitionDto } from '../models/ModuleDefinitionDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class ModuleManagementService {
    /**
     * List all modules
     * Returns a list of all available modules and their state.
     * @returns ModuleDefinitionDto OK
     * @throws ApiError
     */
    public static listModules(): CancelablePromise<Array<ModuleDefinitionDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/modules',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get module details
     * Returns details of a specific module by code.
     * @param code
     * @returns ModuleDefinitionDto OK
     * @throws ApiError
     */
    public static getModule(
        code: string,
    ): CancelablePromise<ModuleDefinitionDto> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/modules/{code}',
            path: {
                'code': code,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
}
