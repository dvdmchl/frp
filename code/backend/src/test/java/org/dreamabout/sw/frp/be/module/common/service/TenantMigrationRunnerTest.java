package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantMigrationRunnerTest {

    @Mock
    private SchemaRepository schemaRepository;

    @Mock
    private SchemaInitializationService schemaInitializationService;

    @InjectMocks
    private TenantMigrationRunner tenantMigrationRunner;

    @Test
    void run_shouldMigrateAllSchemas() {
        var schema1 = new SchemaEntity();
        schema1.setName("schema1");
        var schema2 = new SchemaEntity();
        schema2.setName("schema2");

        when(schemaRepository.findAll()).thenReturn(List.of(schema1, schema2));

        tenantMigrationRunner.run();

        verify(schemaInitializationService).initSchema("schema1");
        verify(schemaInitializationService).initSchema("schema2");
    }

    @Test
    void run_shouldContinueOnFailure() {
        var schema1 = new SchemaEntity();
        schema1.setName("schema1");
        var schema2 = new SchemaEntity();
        schema2.setName("schema2");

        when(schemaRepository.findAll()).thenReturn(List.of(schema1, schema2));
        doThrow(new RuntimeException("Migration failed")).when(schemaInitializationService).initSchema("schema1");

        tenantMigrationRunner.run();

        verify(schemaInitializationService).initSchema("schema1");
        verify(schemaInitializationService).initSchema("schema2");
    }
}
