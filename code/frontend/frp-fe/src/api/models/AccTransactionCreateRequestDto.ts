/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AccJournalCreateRequestDto } from './AccJournalCreateRequestDto';
export type AccTransactionCreateRequestDto = {
    reference?: string;
    description?: string;
    fxRate?: number;
    journals: Array<AccJournalCreateRequestDto>;
};

