/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AccAccountCreateRequestDto } from '../models/AccAccountCreateRequestDto';
import type { AccConflictResolutionRequestDto } from '../models/AccConflictResolutionRequestDto';
import type { AccConnectionCreateRequestDto } from '../models/AccConnectionCreateRequestDto';
import type { AccConnectionCredentialsRequestDto } from '../models/AccConnectionCredentialsRequestDto';
import type { AccConnectionDto } from '../models/AccConnectionDto';
import type { AccConnectionEnabledRequestDto } from '../models/AccConnectionEnabledRequestDto';
import type { AccConnectionFallbackRequestDto } from '../models/AccConnectionFallbackRequestDto';
import type { AccConnectionUpdateRequestDto } from '../models/AccConnectionUpdateRequestDto';
import type { AccConnectorDto } from '../models/AccConnectorDto';
import type { AccCreateMissingAccountsRequestDto } from '../models/AccCreateMissingAccountsRequestDto';
import type { AccCurrencyCreateRequestDto } from '../models/AccCurrencyCreateRequestDto';
import type { AccCurrencyDto } from '../models/AccCurrencyDto';
import type { AccCurrencyUpdateRequestDto } from '../models/AccCurrencyUpdateRequestDto';
import type { AccExternalMappingDto } from '../models/AccExternalMappingDto';
import type { AccExternalMappingUpdateRequestDto } from '../models/AccExternalMappingUpdateRequestDto';
import type { AccImportRecordDto } from '../models/AccImportRecordDto';
import type { AccJournalDto } from '../models/AccJournalDto';
import type { AccJournalUpdateRequestDto } from '../models/AccJournalUpdateRequestDto';
import type { AccNodeDto } from '../models/AccNodeDto';
import type { AccNodeMoveRequestDto } from '../models/AccNodeMoveRequestDto';
import type { AccSyncRunDto } from '../models/AccSyncRunDto';
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Get connection
     * Returns a connection by ID; credentials are never returned.
     * @param id
     * @returns AccConnectionDto OK
     * @throws ApiError
     */
    public static getConnection(
        id: number,
    ): CancelablePromise<AccConnectionDto> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/connections/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Update connection
     * Changes the name and synchronization settings.
     * @param id
     * @param requestBody
     * @returns AccConnectionDto OK
     * @throws ApiError
     */
    public static updateConnection(
        id: number,
        requestBody: AccConnectionUpdateRequestDto,
    ): CancelablePromise<AccConnectionDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/connections/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Delete connection
     * Deletes a connection; its posted transactions stay.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static deleteConnection(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/accounting/connections/{id}',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Update mapping
     * Maps an external account or category to an account, or unmaps or ignores it.
     * @param id
     * @param mappingId
     * @param requestBody
     * @returns AccExternalMappingDto OK
     * @throws ApiError
     */
    public static updateMapping(
        id: number,
        mappingId: number,
        requestBody: AccExternalMappingUpdateRequestDto,
    ): CancelablePromise<AccExternalMappingDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/connections/{id}/mappings/{mappingId}',
            path: {
                'id': id,
                'mappingId': mappingId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Set fallback accounts
     * Sets the accounts for records whose category is not mapped.
     * @param id
     * @param requestBody
     * @returns AccConnectionDto OK
     * @throws ApiError
     */
    public static setFallbackAccounts(
        id: number,
        requestBody: AccConnectionFallbackRequestDto,
    ): CancelablePromise<AccConnectionDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/connections/{id}/fallback-accounts',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Enable or disable connection
     * A disabled connection is not synchronized.
     * @param id
     * @param requestBody
     * @returns AccConnectionDto OK
     * @throws ApiError
     */
    public static setEnabled(
        id: number,
        requestBody: AccConnectionEnabledRequestDto,
    ): CancelablePromise<AccConnectionDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/connections/{id}/enabled',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Set connection credentials
     * Sets or rotates the write-only credentials after the source accepted them.
     * @param id
     * @param requestBody
     * @returns AccConnectionDto OK
     * @throws ApiError
     */
    public static setCredentials(
        id: number,
        requestBody: AccConnectionCredentialsRequestDto,
    ): CancelablePromise<AccConnectionDto> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/accounting/connections/{id}/credentials',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Get connections
     * Returns all connections to external sources.
     * @returns AccConnectionDto OK
     * @throws ApiError
     */
    public static getConnections(): CancelablePromise<Array<AccConnectionDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/connections',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Create connection
     * Creates a connection after the source accepted its credentials.
     * @param requestBody
     * @returns AccConnectionDto OK
     * @throws ApiError
     */
    public static createConnection(
        requestBody: AccConnectionCreateRequestDto,
    ): CancelablePromise<AccConnectionDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Test connection
     * Verifies the stored credentials against the source.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static testConnection(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections/{id}/test',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Synchronize connection
     * Starts a synchronization in the background; its outcome appears in the run history.
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public static syncNow(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections/{id}/sync',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Retry import record
     * Posts a failed record again together with the other pending records of the connection.
     * @param id
     * @param recordId
     * @returns AccImportRecordDto OK
     * @throws ApiError
     */
    public static retryRecord(
        id: number,
        recordId: number,
    ): CancelablePromise<AccImportRecordDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections/{id}/records/{recordId}/retry',
            path: {
                'id': id,
                'recordId': recordId,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Resolve import record conflict
     * Keeps the transaction as changed in FRP, or applies the record from the source.
     * @param id
     * @param recordId
     * @param requestBody
     * @returns AccImportRecordDto OK
     * @throws ApiError
     */
    public static resolveConflict(
        id: number,
        recordId: number,
        requestBody: AccConflictResolutionRequestDto,
    ): CancelablePromise<AccImportRecordDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections/{id}/records/{recordId}/resolve',
            path: {
                'id': id,
                'recordId': recordId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Ignore import record
     * Skips a record waiting to be posted until it changes in the source.
     * @param id
     * @param recordId
     * @returns AccImportRecordDto OK
     * @throws ApiError
     */
    public static ignoreRecord(
        id: number,
        recordId: number,
    ): CancelablePromise<AccImportRecordDto> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections/{id}/records/{recordId}/ignore',
            path: {
                'id': id,
                'recordId': recordId,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Refresh mappings
     * Fetches the external accounts and categories from the source; existing mappings stay.
     * @param id
     * @returns AccExternalMappingDto OK
     * @throws ApiError
     */
    public static refreshMappings(
        id: number,
    ): CancelablePromise<Array<AccExternalMappingDto>> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections/{id}/mappings/refresh',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Create missing accounts
     * Creates and maps an account for every unmapped, not ignored external account or category.
     * @param id
     * @param requestBody
     * @returns AccExternalMappingDto OK
     * @throws ApiError
     */
    public static createMissingAccounts(
        id: number,
        requestBody: AccCreateMissingAccountsRequestDto,
    ): CancelablePromise<Array<AccExternalMappingDto>> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/accounting/connections/{id}/mappings/create-missing-accounts',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
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
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Get connectors
     * Returns the connector types available for new connections with their credential fields.
     * @returns AccConnectorDto OK
     * @throws ApiError
     */
    public static getConnectors(): CancelablePromise<Array<AccConnectorDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/connectors',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Get synchronization runs
     * Returns the synchronization history of a connection, the latest first.
     * @param id
     * @returns AccSyncRunDto OK
     * @throws ApiError
     */
    public static getRuns(
        id: number,
    ): CancelablePromise<Array<AccSyncRunDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/connections/{id}/runs',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Get import records
     * Returns the records staged from a connection in the given statuses, the latest first. Records waiting for a mapping are NEW.
     * @param id
     * @param status
     * @returns AccImportRecordDto OK
     * @throws ApiError
     */
    public static getRecords(
        id: number,
        status: Array<'NEW' | 'POSTED' | 'SKIPPED' | 'ERROR' | 'CONFLICT' | 'DELETED'>,
    ): CancelablePromise<Array<AccImportRecordDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/connections/{id}/records',
            path: {
                'id': id,
            },
            query: {
                'status': status,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Get mappings
     * Returns the external accounts and categories of a connection with their mapping.
     * @param id
     * @returns AccExternalMappingDto OK
     * @throws ApiError
     */
    public static getMappings(
        id: number,
    ): CancelablePromise<Array<AccExternalMappingDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/connections/{id}/mappings',
            path: {
                'id': id,
            },
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
    /**
     * Get account tree
     * Returns the entire account tree.
     * @returns AccNodeDto OK
     * @throws ApiError
     */
    public static getTree(): CancelablePromise<Array<AccNodeDto>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/accounting/accounts/tree',
            errors: {
                400: `Bad Request`,
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Conflict`,
                422: `Unprocessable Content`,
                429: `Too Many Requests`,
                500: `Internal Server Error`,
                502: `Bad Gateway`,
            },
        });
    }
}
