/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { UserChangePasswordRequestDto } from '../models/UserChangePasswordRequestDto';
import type { UserDto } from '../models/UserDto';
import type { UserLoginRequestDto } from '../models/UserLoginRequestDto';
import type { UserLoginResponseDto } from '../models/UserLoginResponseDto';
import type { UserRegisterRequestDto } from '../models/UserRegisterRequestDto';
import type { UserUpdateInfoRequestDto } from '../models/UserUpdateInfoRequestDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class UserManagementService {
    /**
     * Change password
     * Changes the authenticated user's password.
     * @param requestBody
     * @returns any Password changed successfully
     * @throws ApiError
     */
    public static changeAuthenticatedUserPassword(
        requestBody: UserChangePasswordRequestDto,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/user/me/password',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Old password does not match`,
                401: `Unauthorized`,
                403: `User not authenticated`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Update personal info
     * Updates the authenticated user's personal information.
     * @param requestBody
     * @returns UserDto User updated successfully
     * @throws ApiError
     */
    public static updateAuthenticatedUserInfo(
        requestBody: UserUpdateInfoRequestDto,
    ): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/user/me/info',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `User not authenticated`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Register new user
     * Creates a new user account.
     * @param requestBody
     * @returns UserDto User registered successfully
     * @throws ApiError
     */
    public static register(
        requestBody: UserRegisterRequestDto,
    ): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/user/register',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Invalid input`,
                401: `Unauthorized`,
                409: `User with email already exists`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Logout user
     * Logs out the currently authenticated user.
     * @returns void
     * @throws ApiError
     */
    public static logout(): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/user/logout',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `User not authenticated`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Authenticate user
     * Logs in a user and returns user info.
     * @param requestBody
     * @returns UserLoginResponseDto Login successful
     * @throws ApiError
     */
    public static login(
        requestBody: UserLoginRequestDto,
    ): CancelablePromise<UserLoginResponseDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/user/login',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Invalid credentials`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get authenticated user
     * Returns the currently authenticated user.
     * @returns UserDto Authenticated user retrieved successfully
     * @throws ApiError
     */
    public static authenticatedUser(): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/user/me',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `User not authenticated`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
}
