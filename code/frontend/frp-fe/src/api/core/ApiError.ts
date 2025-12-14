/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiRequestOptions } from './ApiRequestOptions';
import type { ApiResult } from './ApiResult';
import type { ErrorDto } from '../models/ErrorDto';

export class ApiError extends Error {
    public readonly url: string;
    public readonly status: number;
    public readonly statusText: string;
    public readonly body: any;
    public readonly request: ApiRequestOptions;
    public readonly errorDto: ErrorDto | undefined;

    constructor(request: ApiRequestOptions, response: ApiResult, message: string) {
        super(message);

        this.name = 'ApiError';
        this.url = response.url;
        this.status = response.status;
        this.statusText = response.statusText;
        this.body = response.body;
        this.request = request;
        if (this.body && typeof this.body === 'object' && ('message' in this.body || 'type' in this.body || 'stackTrace' in this.body)) {
            this.errorDto = this.body as ErrorDto;
            this.message = this.errorDto.message || message;
        }
    
    }
}
