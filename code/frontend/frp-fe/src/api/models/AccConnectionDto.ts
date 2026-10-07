/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AccConnectionDto = {
    id?: number;
    connectorType?: string;
    name?: string;
    enabled?: boolean;
    credentialsSet?: boolean;
    syncSettings?: Record<string, string>;
    lastSuccessfulSyncAt?: string;
    fallbackExpenseAccountId?: number;
    fallbackRevenueAccountId?: number;
    syncIntervalMinutes?: number;
    nextSyncAt?: string;
    credentialsRejected?: boolean;
};

