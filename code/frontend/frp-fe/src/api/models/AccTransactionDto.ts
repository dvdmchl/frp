/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AccJournalDto } from './AccJournalDto';
export type AccTransactionDto = {
    id?: number;
    reference?: string;
    description?: string;
    fxRate?: number;
    totalAmount?: number;
    journals?: Array<AccJournalDto>;
};

