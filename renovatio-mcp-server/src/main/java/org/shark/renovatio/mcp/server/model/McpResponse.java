package org.shark.renovatio.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "MCP JSON-RPC 2.0 Response")
@Data
public class McpResponse {
    @JsonProperty("jsonrpc")
    @Schema(description = "JSON-RPC version", example = "2.0")
    private String jsonrpc = "2.0";

    @Schema(description = "Request ID")
    private String id;

    @Schema(description = "Response result")
    private Object result;

    @Schema(description = "Error object if request failed")
    private McpError error;

    public McpResponse() {
    }

    public McpResponse(String id, Object result) {
        this.id = id;
        this.result = result;
    }

    public McpResponse(String id, McpError error) {
        this.id = id;
        this.error = error;
    }
}