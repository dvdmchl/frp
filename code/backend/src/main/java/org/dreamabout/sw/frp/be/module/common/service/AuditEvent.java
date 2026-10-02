package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;

/**
 * Application event recorded into the audit log once the publishing transaction commits.
 *
 * @param userId    acting user; {@code null} means the currently authenticated user (if any)
 * @param userEmail acting user's email; {@code null} means the currently authenticated user (if any)
 */
public record AuditEvent(AuditAction action, Long userId, String userEmail, String resource, String details) {

    public static AuditEvent ofCurrentUser(AuditAction action, String resource, String details) {
        return new AuditEvent(action, null, null, resource, details);
    }

    public static AuditEvent ofUser(AuditAction action, Long userId, String userEmail, String details) {
        return new AuditEvent(action, userId, userEmail, userResource(userId), details);
    }

    public static String userResource(Long userId) {
        return userId == null ? null : "user:" + userId;
    }

    public static String schemaResource(String schemaName) {
        return "schema:" + schemaName;
    }
}
