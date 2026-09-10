package org.shark.renovatio.llm.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Redacts sensitive data from governance logs.
 * Removes PII, API keys, and sensitive fields from context before logging.
 */
@Component
public class GovernanceRedactor {

    // Fields that should never appear in logs
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "secret", "token", "apiKey", "api_key", "apikey",
        "accessToken", "access_token", "refreshToken", "refresh_token",
        "privateKey", "private_key", "certificate", "cert",
        "ssn", "socialSecurity", "creditCard", "credit_card",
        "email", "phone", "address", "dob", "dateOfBirth"
    );

    // Patterns for PII detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(\\+?\\d{1,3}[-.\s]?)?\\(?\\d{3}\\)?[-.\s]?\\d{3}[-.\s]?\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[-.\s]?\\d{4}[-.\s]?\\d{4}[-.\s]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[_-]?key|access[_-]?token|secret)[\"\\s:=]+[\"\\s]*([a-zA-Z0-9._-]{20,})");

    // Maximum allowed length for any string value in logs
    private static final int MAX_STRING_LENGTH = 500;
    // Maximum allowed depth for nested objects
    private static final int MAX_DEPTH = 5;

    /**
     * Redacts a context map for safe logging.
     */
    public Map<String, Object> redactForLog(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return Map.of();
        }

        // 1. Filter to allowed fields (remove sensitive)
        Map<String, Object> filtered = filterSensitiveFields(context);

        // 2. Redact PII in string values
        Map<String, Object> redacted = redactPII(filtered);

        // 3. Truncate long strings
        Map<String, Object> truncated = truncateStrings(redacted);

        // 4. Limit number of entries
        Map<String, Object> limited = limitEntries(redacted);

        // 5. Ensure total size is within limits
        return ensureSizeLimit(limited);
    }

    private Map<String, Object> filterSensitiveFields(Map<String, Object> context) {
        return context.entrySet().stream()
            .filter(e -> !isSensitiveField(e.getKey()))
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
    }

    private boolean isSensitiveField(String key) {
        String lower = key.toLowerCase();
        return SENSITIVE_FIELDS.stream().anyMatch(lower::contains);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> redactPII(Map<String, Object> context) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                result.put(entry.getKey(), null);
            } else if (value instanceof String str) {
                result.put(entry.getKey(), redactPIIInString(str));
            } else if (value instanceof Map) {
                result.put(entry.getKey(), redactPII((Map<String, Object>) value));
            } else if (value instanceof Iterable<?> iterable) {
                result.put(entry.getKey(), redactPIIInIterable(iterable));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private String redactPIIInString(String str) {
        String result = str;
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL_REDACTED]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE_REDACTED]");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[CARD_REDACTED]");
        result = SSN_PATTERN.matcher(result).replaceAll("[SSN_REDACTED]");
        result = API_KEY_PATTERN.matcher(result).replaceAll("$1=[REDACTED]");
        return result;
    }

    private java.util.List<Object> redactPIIInIterable(Iterable<?> iterable) {
        java.util.List<Object> result = new java.util.ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof String str) {
                result.add(redactPIIInString(str));
            } else if (item instanceof Map) {
                result.add(redactPII((Map<String, Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Object> truncateStrings(Map<String, Object> context) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str && str.length() > 500) {
                result.put(entry.getKey(), str.substring(0, 500) + "...[TRUNCATED]");
            } else if (value instanceof Map) {
                result.put(entry.getKey(), truncateStrings((Map<String, Object>) value));
            } else if (value instanceof Iterable<?> iterable) {
                result.put(entry.getKey(), truncateIterable(iterable));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private java.util.List<Object> truncateIterable(Iterable<?> iterable) {
        java.util.List<Object> result = new java.util.ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof String str && str.length() > 500) {
                result.add(str.substring(0, 500) + "...[TRUNCATED]");
            } else if (item instanceof Map) {
                result.add(truncateStrings((Map<String, Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Object> limitEntries(Map<String, Object> context) {
        if (context.size() <= 20) {
            return context;
        }
        return context.entrySet().stream()
            .limit(20)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
    }

    private Map<String, Object> ensureSizeLimit(Map<String, Object> context) {
        String json = toJsonString(context);
        if (json.length() <= 5000) {
            return context;
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>(context);
        while (toJsonString(result).length() > 5000 && !result.isEmpty()) {
            result.remove(result.keySet().iterator().next());
        }
        return result;
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return map.toString();
        }
    }
}