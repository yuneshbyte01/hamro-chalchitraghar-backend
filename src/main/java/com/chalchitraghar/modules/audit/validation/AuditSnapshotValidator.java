package com.chalchitraghar.modules.audit.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AuditSnapshotValidator {
    public static final int MAX_JSON_BYTES = 16 * 1024;
    private static final int MAX_VALUE_LENGTH = 2_000;
    private static final Set<String> ALLOWED_KEYS =
            Set.of(
                    "id",
                    "status",
                    "role",
                    "email",
                    "name",
                    "title",
                    "type",
                    "reference",
                    "amount",
                    "currency",
                    "provider",
                    "method",
                    "reason",
                    "channel",
                    "enabled",
                    "oldValue",
                    "newValue",
                    "count",
                    "source",
                    "version",
                    "result",
                    "action",
                    "category",
                    "resourceType",
                    "resourceId",
                    "resourceReference");
    private static final Set<String> FORBIDDEN_PARTS =
            Set.of(
                    "password",
                    "passwordhash",
                    "otp",
                    "resettoken",
                    "jwt",
                    "refreshtoken",
                    "googletoken",
                    "oauthtoken",
                    "qrtoken",
                    "qrsigningkey",
                    "paymentsecret",
                    "paymentsignature",
                    "smtpcredential",
                    "smtpPassword",
                    "rawpaymentresponse",
                    "requestbody",
                    "responsebody",
                    "binarycontent",
                    "pdfbytes",
                    "emailbody",
                    "stacktrace",
                    "sqlstatement",
                    "secret",
                    "credential",
                    "token");

    private final ObjectMapper objectMapper;

    public AuditSnapshotValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String validateAndSerialize(Map<String, ?> snapshot, String field) {
        if (snapshot == null) return null;
        validateMap(snapshot, field);
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES)
                throw new IllegalArgumentException(field + " exceeds 16 KB");
            return json;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(field + " must contain JSON-compatible values");
        }
    }

    private void validateMap(Map<?, ?> map, String field) {
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank())
                throw new IllegalArgumentException(field + " contains an invalid key");
            String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
            if (FORBIDDEN_PARTS.stream().anyMatch(normalized::contains))
                throw new IllegalArgumentException(field + " contains a forbidden key");
            if (!ALLOWED_KEYS.contains(key))
                throw new IllegalArgumentException(
                        field + " contains a key that is not allowlisted");
            validateValue(entry.getValue(), field);
        }
    }

    private void validateValue(Object value, String field) {
        if (value instanceof Map<?, ?> nested) validateMap(nested, field);
        else if (value instanceof Collection<?> values)
            values.forEach(item -> validateValue(item, field));
        else if (value instanceof String text && text.length() > MAX_VALUE_LENGTH)
            throw new IllegalArgumentException(field + " contains an oversized value");
        else if (value != null
                && !(value instanceof Number
                        || value instanceof Boolean
                        || value instanceof Enum<?>
                        || value instanceof String))
            throw new IllegalArgumentException(field + " contains a non-scalar value");
    }
}
