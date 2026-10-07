package org.dreamabout.sw.frp.be.module.accounting.connector;

import java.util.List;
import java.util.Optional;

/**
 * SPI for an external source of accounting data (e.g. BudgetBakers Wallet, a bank).
 *
 * <p>Implementations are Spring beans collected by {@link ConnectorRegistry}. A connector is read-only towards the
 * source: it only fetches data and translates it into source-neutral records. Persisting the data is the job of the
 * import pipeline, so connectors must not use accounting repositories.
 *
 * <p>Failures are reported as {@link ConnectorException} subtypes.
 */
public interface AccountingConnector {

    /**
     * Stable code of the source (e.g. {@code WALLET}); stored with connections, so it must never change.
     */
    String type();

    /**
     * Credential fields a connection to this source needs.
     */
    List<CredentialField> credentialFields();

    /**
     * Verifies that the source accepts the credentials; throws a {@link ConnectorException} otherwise.
     */
    void testConnection(ConnectorCredentials credentials);

    /**
     * Revision of the data in the source, changing with every change of it. The import pipeline skips a
     * synchronization when the revision is the same as at the start of the last finished one.
     *
     * @return empty when the source does not tell (every synchronization then fetches the records)
     */
    default Optional<String> dataRevision(ConnectorCredentials credentials) {
        return Optional.empty();
    }

    List<ExternalAccount> fetchAccounts(ConnectorCredentials credentials);

    List<ExternalCategory> fetchCategories(ConnectorCredentials credentials);

    /**
     * Fetches one page of records in the window.
     *
     * @param cursor {@code null} for the first page, otherwise {@link RecordPage#nextCursor()} of the previous page
     */
    RecordPage fetchRecords(ConnectorCredentials credentials, RecordWindow window, String cursor);
}
