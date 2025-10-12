package org.shark.renovatio.provider.cobol.domain;

import lombok.Data;

import java.util.Map;

/**
 * Simple MCP Tool representation for COBOL provider
 * This is a simplified version for use until core MCP classes are available
 */
@Data
public class CobolMcpTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;

    public CobolMcpTool() {
    }

    public CobolMcpTool(String name, String description) {
        this.name = name;
        this.description = description;
    }
}