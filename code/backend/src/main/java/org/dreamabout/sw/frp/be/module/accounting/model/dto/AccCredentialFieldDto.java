package org.dreamabout.sw.frp.be.module.accounting.model.dto;

/**
 * @param secret whether the value is secret, e.g. an API token; such a value must be entered as a password
 */
public record AccCredentialFieldDto(
    String name,
    boolean secret
) {}
