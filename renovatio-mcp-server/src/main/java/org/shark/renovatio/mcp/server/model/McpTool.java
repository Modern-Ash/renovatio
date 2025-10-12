package org.shark.renovatio.mcp.server.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * MCP Tool model representing a tool available through the MCP protocol
 */
@Data
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;
    private Map<String, Object> outputSchema;
    private List<Map<String, Object>> parameters;
    private Map<String, Object> example;
    private Map<String, Object> metadata;

    public McpTool() {
        this.metadata = new LinkedHashMap<>();
    }

    public McpTool(String name, String description, Map<String, Object> inputSchema, Map<String, Object> outputSchema,
                   List<Map<String, Object>> parameters, Map<String, Object> example) {
        this(name, description, inputSchema, outputSchema, parameters, example, null);
    }

    public McpTool(String name, String description, Map<String, Object> inputSchema, Map<String, Object> outputSchema,
                   List<Map<String, Object>> parameters, Map<String, Object> example, Map<String, Object> metadata) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.parameters = parameters;
        this.example = example;
        this.metadata = metadata != null ? new LinkedHashMap<>(metadata) : new LinkedHashMap<>();
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new LinkedHashMap<>(metadata) : new LinkedHashMap<>();
    }
}
