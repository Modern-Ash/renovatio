package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualActionItemTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesOnlySchemaEnumValues() {
        assertThat(objectMapper.valueToTree(GuardrailGate.REVIEW_ELIGIBILITY).asText())
                .isEqualTo("review-eligibility");
        assertThat(objectMapper.valueToTree(ManualActionSeverity.CRITICAL).asText())
                .isEqualTo("critical");
        assertThat(objectMapper.valueToTree(ManualActionReviewStatus.RESOLVED).asText())
                .isEqualTo("resolved");
    }
}
