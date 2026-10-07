package org.dreamabout.sw.frp.be.module.accounting.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * SHA-256 fingerprint of an ordered list of fields, used to detect changes of content. Numbers are compared by value,
 * so {@code 100} and {@code 100.0000} give the same fingerprint.
 */
final class ContentHash {

    private static final char FIELD_SEPARATOR = '\u001f';
    private static final String NULL_FIELD = "\u0000";

    private ContentHash() {
    }

    static String of(Stream<?> fields) {
        return HexFormat.of().formatHex(sha256().digest(join(fields).getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Fields as one string, e.g. as a sortable key of a nested element whose order does not matter.
     */
    static String join(Stream<?> fields) {
        var content = new StringBuilder();
        fields.forEach(field -> content.append(asText(field)).append(FIELD_SEPARATOR));
        return content.toString();
    }

    private static String asText(Object field) {
        if (field instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }
        return Objects.toString(field, NULL_FIELD);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
