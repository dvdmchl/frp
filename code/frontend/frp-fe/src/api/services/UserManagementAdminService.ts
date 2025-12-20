/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { UserDto } from '../models/UserDto';
import type { UserUpdateGroupsRequestDto } from '../models/UserUpdateGroupsRequestDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class UserManagementAdminService {
    /**
     * Update user groups
     * Sets the groups for a user.
     * @param id
     * @param requestBody
     * @returns UserDto OK
     * @throws ApiError
     */
    public static updateGroups(
        id: number,
        requestBody: UserUpdateGroupsRequestDto,
    ): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/admin/users/{id}/groups',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Grant/Revoke admin status
     * Toggles the user's admin privilege.
     * @param id
     * @param admin
     * @returns UserDto OK
     * @throws ApiError
     */
    public static updateAdminStatus(
        id: number,
        admin: boolean,
    ): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/admin/users/{id}/admin',
            path: {
                'id': id,
            },
            query: {
                'admin': admin,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Activate/Deactivate user
     * Toggles the user's active status.
     * @param id
     * @param active
     * @returns UserDto OK
     * @throws ApiError
     */
    public static updateActiveStatus(
        id: number,
        active: boolean,
    ): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/admin/users/{id}/active',
            path: {
                'id': id,
            },
            query: {
                'active': active,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * List/Search users
     * Returns all users or searches by email/name.
     * @param query
     * @returns UserDto OK
     * @throws ApiError
     */
    public static listUsers(
        query?: string,
    ): CancelablePromise<Array<UserDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/admin/users',
            query: {
                'query': query,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get user details
     * Returns detailed information about a user.
     * @param id
     * @returns UserDto OK
     * @throws ApiError
     */
    public static getUser(
        id: number,
    ): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/admin/users/{id}',
            path: {
                'id': id,
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
