package org.shark.renovatio.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aggregated metrics reported by tools.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metrics {
    @JsonProperty("values")
    private Map<String, Object> values;
}
