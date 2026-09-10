package org.shark.renovatio.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsDto {
    private Map<String, Double> metrics;
    private Map<String, Object> details;
}
