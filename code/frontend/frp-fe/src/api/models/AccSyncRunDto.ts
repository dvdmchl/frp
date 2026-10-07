/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AccSyncRunDto = {
    id?: number;
    trigger?: 'SCHEDULED' | 'MANUAL';
    status?: 'RUNNING' | 'SUCCESS' | 'PARTIAL' | 'FAILED';
    startedAt?: string;
    finishedAt?: string;
    fetched?: number;
    created?: number;
    updated?: number;
    deleted?: number;
    posted?: number;
    errors?: number;
    errorMessage?: string;
};

