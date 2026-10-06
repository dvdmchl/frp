package org.dreamabout.sw.frp.be.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.dreamabout.sw.frp.be.module.accounting.connector.AccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.dreamabout.sw.frp.be.module.accounting.connector.CredentialField;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAccount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalCategory;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordPage;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountingConnectorRuleTest {

    @Test
    void shouldRejectConnectorThatUsesAccountingRepository() {
        var classes = new ClassFileImporter().importClasses(RepositoryUsingConnector.class);

        assertThatThrownBy(() -> ArchitectureTest.accounting_connectors_must_not_access_repositories.check(classes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("AccAccountRepository");
    }

    private record RepositoryUsingConnector(AccAccountRepository repository) implements AccountingConnector {

        @Override
        public String type() {
            return "BAD";
        }

        @Override
        public List<CredentialField> credentialFields() {
            return List.of();
        }

        @Override
        public void testConnection(ConnectorCredentials credentials) {
            repository.count();
        }

        @Override
        public List<ExternalAccount> fetchAccounts(ConnectorCredentials credentials) {
            return List.of();
        }

        @Override
        public List<ExternalCategory> fetchCategories(ConnectorCredentials credentials) {
            return List.of();
        }

        @Override
        public RecordPage fetchRecords(ConnectorCredentials credentials, RecordWindow window, String cursor) {
            return RecordPage.last(List.of());
        }
    }
}
