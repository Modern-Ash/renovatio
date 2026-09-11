package org.shark.renovatio.llm.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sanitizes prompts to prevent prompt injection attacks.
 * Removes or neutralizes potentially dangerous patterns.
 */
@Component
public class PromptSanitizer {

    // Patterns that could indicate prompt injection attempts
    private static final Pattern[] INJECTION_PATTERNS = {
        Pattern.compile("(?i)ignore\\s+(previous|above|all).*?instructions"),
        Pattern.compile("(?i)forget\\s+(everything|previous|above)"),
        Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an)\\s+"),
        Pattern.compile("(?i)system\\s*:\\s*"),
        Pattern.compile("(?i)assistant\\s*:\\s*"),
        Pattern.compile("(?i)<\\[.*\\]>"),  // XML-style injection markers
        Pattern.compile("(?i)```.*```"),     // Code block injection
        Pattern.compile("(?i)new\\s+(instruction|task|role)"),
        Pattern.compile("(?i)override\\s+(safety|security|policy)"),
        Pattern.compile("(?i)jailbreak"),
        Pattern.compile("(?i)prompt\\s+injection"),
    };

    // Maximum allowed length for any string value in context
    private static final int MAX_STRING_LENGTH = 10000;

    // Maximum allowed depth for nested objects
    private static final int MAX_DEPTH = 10;

    /**
     * Sanitizes a prompt string by removing potential injection patterns.
     */
    public String sanitize(String prompt) {
        if (prompt == null) {
            return "";
        }

        String sanitized = prompt;
        for (Pattern pattern : INJECTION_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]");
        }

        // Truncate if too long
        if (sanitized.length() > MAX_STRING_LENGTH) {
            sanitized = sanitized.substring(0, MAX_STRING_LENGTH) + "...[TRUNCATED]";
        }

        return sanitized;
    }

    /**
     * Sanitizes a context map by recursively cleaning string values.
     */
    public Map<String, Object> sanitizeContext(Map<String, Object> context) {
        if (context == null) {
            return Map.of();
        }
        return sanitizeContextRecursive(context, 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeContextRecursive(Map<String, Object> context, int depth) {
        if (depth > MAX_DEPTH) {
            return Map.of("error", "Max depth exceeded");
        }

        Map<String, Object> sanitized = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                sanitized.put(entry.getKey(), null);
            } else if (value instanceof String str) {
                sanitized.put(entry.getKey(), sanitize(str));
            } else if (value instanceof Map) {
                sanitized.put(entry.getKey(), sanitizeContextRecursive((Map<String, Object>) value, depth + 1));
            } else if (value instanceof Iterable<?> iterable) {
                sanitized.put(entry.getKey(), sanitizeIterable(iterable, depth + 1));
            } else {
                sanitized.put(entry.getKey(), value);
            }
        }
        return sanitized;
    }

    private java.util.List<Object> sanitizeIterable(Iterable<?> iterable, int depth) {
        java.util.List<Object> result = new java.util.ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof String str) {
                result.add(sanitize(str));
            } else if (item instanceof Map) {
                result.add(sanitizeContextRecursive((Map<String, Object>) item, depth + 1));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Checks if a prompt contains potential injection patterns.
     * Used for monitoring and alerting.
     */
    public boolean hasInjectionPatterns(String prompt) {
        if (prompt == null) {
            return false;
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                return true;
            }
        }
        return false;
    }
}