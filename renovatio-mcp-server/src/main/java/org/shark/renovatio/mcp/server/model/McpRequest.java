package org.shark.renovatio.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "MCP JSON-RPC 2.0 Request")
@Data
public class McpRequest {
    @JsonProperty("jsonrpc")
    @Schema(description = "JSON-RPC version", example = "2.0")
    private String jsonrpc = "2.0";

    @Schema(description = "Request ID")
    private String id;

    @Schema(description = "Method name")
    private String method;

    @Schema(description = "Request parameters")
    private Object params;

    public McpRequest() {
    }

    public McpRequest(String id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    @Override
    public String toString() {
        return "McpRequest{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", id='" + id + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                '}';
    }
}