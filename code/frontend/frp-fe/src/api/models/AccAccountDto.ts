/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AccAccountDto = {
    id?: number;
    name?: string;
    description?: string;
    currencyCode?: string;
    isLiquid?: boolean;
    accountType?: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';
    balance?: number;
};

