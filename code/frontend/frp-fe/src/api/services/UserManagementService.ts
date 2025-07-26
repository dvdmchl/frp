/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { UserDto } from '../models/UserDto';
import type { UserLoginRequestDto } from '../models/UserLoginRequestDto';
import type { UserLoginResponseDto } from '../models/UserLoginResponseDto';
import type { UserRegisterRequestDto } from '../models/UserRegisterRequestDto';
import type { UserUpdateRequestDto } from '../models/UserUpdateRequestDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class UserManagementService {
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
                403: `User not authenticated`,
            },
        });
    }
    /**
     * Update authenticated user
     * Updates the currently authenticated user.
     * @param requestBody
     * @returns UserDto User updated successfully
     * @throws ApiError
     */
    public static updateAuthenticatedUser(
        requestBody: UserUpdateRequestDto,
    ): CancelablePromise<UserDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/user/me',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `User not authenticated`,
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
                409: `User with email already exists`,
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
                403: `User not authenticated`,
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
                401: `Invalid credentials`,
            },
        });
    }
}
