/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AccImportRecordDto = {
    id?: number;
    externalId?: string;
    externalAccountId?: string;
    externalCategoryId?: string;
    recordDate?: string;
    amount?: number;
    currencyCode?: string;
    note?: string;
    counterparty?: string;
    sourceState?: 'BOOKED' | 'PENDING' | 'DELETED';
    transferLinkId?: string;
    status?: 'NEW' | 'POSTED' | 'SKIPPED' | 'ERROR' | 'CONFLICT' | 'DELETED';
    errorMessage?: string;
    transactionId?: number;
    lastSeenAt?: string;
};

