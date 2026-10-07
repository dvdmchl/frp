/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AccImportRecordDto } from './AccImportRecordDto';
import type { AccMappedItemDto } from './AccMappedItemDto';
export type AccImportRecordDetailDto = {
    importRecord?: AccImportRecordDto;
    account?: AccMappedItemDto;
    category?: AccMappedItemDto;
    transferAccount?: AccMappedItemDto;
    baseAmount?: number;
    baseCurrencyCode?: string;
    sourceUpdatedAt?: string;
    firstSeenAt?: string;
    rawPayload?: string;
};

