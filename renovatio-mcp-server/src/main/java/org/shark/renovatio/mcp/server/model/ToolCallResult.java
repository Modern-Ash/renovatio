package org.shark.renovatio.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * Wrapper for tool execution results returned to MCP clients.
 */
@Data
@NoArgsConstructor
public class ToolCallResult {
    @JsonProperty("content")
    private List<TextContent> content;
    @JsonProperty("structuredContent")
    private Object structuredContent;
    @JsonProperty("isError")
    private boolean isError;

    public ToolCallResult(List<TextContent> content, Object structuredContent, boolean isError) {
        this.content = Objects.requireNonNullElse(content, List.of(new TextContent("")));
        this.structuredContent = structuredContent;
        this.isError = isError;
    }

    public static ToolCallResult ok(String summary, Object structured) {
        return new ToolCallResult(List.of(new TextContent(summary)), structured, false);
    }

    public static ToolCallResult error(String message) {
        return new ToolCallResult(List.of(new TextContent(message)), null, true);
    }

    public static ToolCallResult error(String message, Object structured) {
        return new ToolCallResult(List.of(new TextContent(message)), structured, true);
    }
}
