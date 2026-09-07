package org.shark.renovatio.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record WorkbenchAiDto(List<Item> items) {
    public record Item(String id, String category, String source, String status, BigDecimal confidence, int evidenceCount, boolean llmFailed) { }
}
