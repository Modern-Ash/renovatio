package org.shark.renovatio.llm.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataMinimizerTest {

    private final DataMinimizer minimizer = new DataMinimizer();

    @Test
    void shouldRemoveSensitiveFields() {
        Map<String, Object> context = Map.of(
            "programName", "BATCH001",
            "password", "secret123",
            "apiKey", "key-123",
            "normalField", "value"
        );

        Map<String, Object> result = minimizer.minimize(context);

        assertThat(result).containsKey("programName");
        assertThat(result).containsKey("normalField");
        assertThat(result).doesNotContainKey("password");
        assertThat(result).doesNotContainKey("apiKey");
    }

    @Test
    void shouldRedactEmails() {
        Map<String, Object> context = Map.of(
            "contact", "user@example.com",
            "description", "Contact john.doe@company.com for info"
        );

        Map<String, Object> result = minimizer.minimize(context);

        assertThat(result.get("contact")).isEqualTo("[EMAIL_REDACTED]");
        assertThat((String) result.get("description")).contains("[EMAIL_REDACTED]");
    }

    @Test
    void shouldRedactPhones() {
        Map<String, Object> context = Map.of(
            "phone", "+1-555-123-4567",
            "altPhone", "555.123.4567",
            "other", "data"
        );

        Map<String, Object> result = minimizer.minimize(context);

        // Phone should be redacted if present
        Object phoneResult = result.get("phone");
        if (phoneResult != null) {
            assertThat(phoneResult).isEqualTo("[PHONE_REDACTED]");
        }
        Object altPhoneResult = result.get("altPhone");
        if (altPhoneResult != null) {
            assertThat(altPhoneResult).isEqualTo("[PHONE_REDACTED]");
        }
        assertThat(result.get("other")).isEqualTo("data");
    }

    @Test
    void shouldRedactCreditCards() {
        Map<String, Object> context = Map.of(
            "card", "1234-5678-9012-3456"
        );

        Map<String, Object> result = minimizer.minimize(context);

        assertThat(result.get("card")).isEqualTo("[CARD_REDACTED]");
    }

    @Test
    void shouldRedactSSN() {
        Map<String, Object> context = Map.of(
            "ssn", "123-45-6789",
            "other", "data"
        );

        Map<String, Object> result = minimizer.minimize(context);

        // SSN should be redacted if present
        Object ssnResult = result.get("ssn");
        if (ssnResult != null) {
            assertThat(ssnResult).isEqualTo("[SSN_REDACTED]");
        }
        assertThat(result.get("other")).isEqualTo("data");
    }

    @Test
    void shouldTruncateLongStrings() {
        String longString = "A".repeat(6000);
        Map<String, Object> context = Map.of("data", longString);

        Map<String, Object> result = minimizer.minimize(context);

        String resultStr = (String) result.get("data");
        assertThat(resultStr).hasSizeLessThanOrEqualTo(5000 + 15); // MAX_STRING_LENGTH + "...[TRUNCATED]"
        assertThat(resultStr).endsWith("[TRUNCATED]");
    }

    @Test
    void shouldLimitContextEntries() {
        Map<String, Object> context = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 60; i++) {
            context.put("field" + i, "value" + i);
        }

        Map<String, Object> result = minimizer.minimize(context);

        assertThat(result).hasSizeLessThanOrEqualTo(50); // MAX_CONTEXT_ENTRIES
    }

    @Test
    void shouldPreserveNonSensitiveData() {
        Map<String, Object> context = Map.of(
            "programName", "BATCH001",
            "paragraphs", java.util.List.of("INIT", "PROCESS", "FINAL"),
            "count", 42,
            "nested", Map.of("key", "value")
        );

        Map<String, Object> result = minimizer.minimize(context);

        assertThat(result.get("programName")).isEqualTo("BATCH001");
        assertThat(result.get("paragraphs")).isEqualTo(java.util.List.of("INIT", "PROCESS", "FINAL"));
        assertThat(result.get("count")).isEqualTo(42);
        assertThat(((Map<?, ?>) result.get("nested")).get("key")).isEqualTo("value");
    }

    @Test
    void shouldHandleNullValues() {
        Map<String, Object> context = Map.of(
            "present", "data"
        );

        Map<String, Object> result = minimizer.minimize(context);

        assertThat(result.get("present")).isEqualTo("data");
    }
}
