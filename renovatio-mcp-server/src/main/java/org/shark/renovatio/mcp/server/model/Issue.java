package org.shark.renovatio.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation of an issue reported by MCP tools.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    @JsonProperty("file")
    private String file;
    @JsonProperty("line")
    private Integer line;
    @JsonProperty("severity")
    private String severity;
    @JsonProperty("type")
    private String type;
    @JsonProperty("message")
    private String message;
    @JsonProperty("recipe")
    private String recipe;
}
