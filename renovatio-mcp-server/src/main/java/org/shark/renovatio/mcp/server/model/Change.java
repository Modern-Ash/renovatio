package org.shark.renovatio.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation of a change preview or applied modification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Change {
    @JsonProperty("file")
    private String file;
    @JsonProperty("diff")
    private String diff;
}
