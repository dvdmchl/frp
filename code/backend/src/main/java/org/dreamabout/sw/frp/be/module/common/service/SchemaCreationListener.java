package org.dreamabout.sw.frp.be.module.common.service;

public interface SchemaCreationListener {
    /**
     * Called when a new schema is created and initialized.
     *
     * @param schemaName The name of the new schema.
     * @param ownerId    The ID of the user who owns the schema.
     */
    void onSchemaCreated(String schemaName, Long ownerId);
}
