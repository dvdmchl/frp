/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AccAccountCreateRequestDto } from '../models/AccAccountCreateRequestDto';
import type { AccAccountDto } from '../models/AccAccountDto';
import type { AccCurrencyCreateRequestDto } from '../models/AccCurrencyCreateRequestDto';
import type { AccCurrencyDto } from '../models/AccCurrencyDto';
import type { AccCurrencyUpdateRequestDto } from '../models/AccCurrencyUpdateRequestDto';
import type { AccJournalDto } from '../models/AccJournalDto';
import type { AccJournalUpdateRequestDto } from '../models/AccJournalUpdateRequestDto';
import type { AccNodeDto } from '../models/AccNodeDto';
import type { AccNodeMoveRequestDto } from '../models/AccNodeMoveRequestDto';
import type { AccTransactionCreateRequestDto } from '../models/AccTransactionCreateRequestDto';
import type { AccTransactionDto } from '../models/AccTransactionDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AccountingService {
    /**
     * Get transaction
     * Returns a specific transaction by ID.
     * @param id
     * @returns AccTransactionDto OK
     * @throws ApiError
     */
    public static getTransaction(
        id: number,
    ): CancelablePromise<AccTransactionDto> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/transactions/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Update transaction
     * Updates an existing transaction.
     * @param id
     * @param requestBody
     * @returns AccTransactionDto OK
     * @throws ApiError
     */
    public static updateTransaction(
        id: number,
        requestBody: AccTransactionCreateRequestDto,
    ): CancelablePromise<AccTransactionDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/transactions/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Delete transaction
     * Deletes a transaction.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static deleteTransaction(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/accounting/transactions/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get journal
     * Returns a specific journal entry by ID.
     * @param id
     * @returns AccJournalDto OK
     * @throws ApiError
     */
    public static getJournal(
        id: number,
    ): CancelablePromise<AccJournalDto> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/journals/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Update journal
     * Updates an existing journal entry (date/description only).
     * @param id
     * @param requestBody
     * @returns AccJournalDto OK
     * @throws ApiError
     */
    public static updateJournal(
        id: number,
        requestBody: AccJournalUpdateRequestDto,
    ): CancelablePromise<AccJournalDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/journals/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Delete journal
     * Deletes a journal entry.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static deleteJournal(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/accounting/journals/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Update currency
     * Updates an existing currency.
     * @param id
     * @param requestBody
     * @returns AccCurrencyDto OK
     * @throws ApiError
     */
    public static updateCurrency(
        id: number,
        requestBody: AccCurrencyUpdateRequestDto,
    ): CancelablePromise<AccCurrencyDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/currencies/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Delete currency
     * Deletes a currency if not in use.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static deleteCurrency(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/accounting/currencies/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Set base currency
     * Sets a specific currency as the base currency.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static setBaseCurrency(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/currencies/{id}/active',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Update account
     * Updates an existing account.
     * @param id
     * @param requestBody
     * @returns AccNodeDto OK
     * @throws ApiError
     */
    public static updateAccount(
        id: number,
        requestBody: AccAccountCreateRequestDto,
    ): CancelablePromise<AccNodeDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/accounts/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Delete account
     * Deletes an account node.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static deleteAccount(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/accounting/accounts/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get all transactions
     * Returns a list of all transactions.
     * @returns AccTransactionDto OK
     * @throws ApiError
     */
    public static getAllTransactions(): CancelablePromise<Array<AccTransactionDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/transactions',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Create transaction
     * Creates a new transaction with journal entries.
     * @param requestBody
     * @returns AccTransactionDto OK
     * @throws ApiError
     */
    public static createTransaction(
        requestBody: AccTransactionCreateRequestDto,
    ): CancelablePromise<AccTransactionDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/transactions',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get all currencies
     * Returns a list of all available currencies.
     * @returns AccCurrencyDto OK
     * @throws ApiError
     */
    public static getAllCurrencies(): CancelablePromise<Array<AccCurrencyDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/currencies',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Create currency
     * Creates a new currency.
     * @param requestBody
     * @returns AccCurrencyDto OK
     * @throws ApiError
     */
    public static createCurrency(
        requestBody: AccCurrencyCreateRequestDto,
    ): CancelablePromise<AccCurrencyDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/currencies',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Create account
     * Creates a new account or placeholder node.
     * @param requestBody
     * @returns AccNodeDto OK
     * @throws ApiError
     */
    public static createAccount(
        requestBody: AccAccountCreateRequestDto,
    ): CancelablePromise<AccNodeDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/accounts',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Move account
     * Moves an account to a new parent and/or reorders it.
     * @param id
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public static moveAccount(
        id: number,
        requestBody: AccNodeMoveRequestDto,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/accounts/{id}/move',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get all journals
     * Returns a list of all journal entries.
     * @returns AccJournalDto OK
     * @throws ApiError
     */
    public static getAllJournals(): CancelablePromise<Array<AccJournalDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/journals',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Get account tree
     * Returns the entire account tree.
     * @returns any OK
     * @throws ApiError
     */
    public static getTree(): CancelablePromise<Array<{
        id?: number;
        parentId?: number;
        isPlaceholder?: boolean;
        account?: AccAccountDto;
        orderIndex?: number;
        children?: Array<any>;
    }>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/accounts/tree',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                409: `Conflict`,
                500: `Internal Server Error`,
            },
        });
    }
}
