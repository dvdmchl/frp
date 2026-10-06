package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * One value a connection needs, e.g. an API token. Secret values must never be shown or logged.
 */
public record CredentialField(String name, boolean secret) {

    public static CredentialField secret(String name) {
        return new CredentialField(name, true);
    }

    public static CredentialField plain(String name) {
        return new CredentialField(name, false);
    }
}
