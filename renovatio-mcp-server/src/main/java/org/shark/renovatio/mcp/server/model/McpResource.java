package org.shark.renovatio.mcp.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "MCP Resource metadata")
@Data
public class McpResource {
    @Schema(description = "Resource URI")
    private String uri;

    @Schema(description = "Resource name")
    private String name;

    @Schema(description = "MIME type")
    private String mimeType;

    @Schema(description = "Resource text content")
    private String text;

    public McpResource() {
    }

    public McpResource(String uri, String name, String mimeType, String text) {
        this.uri = uri;
        this.name = name;
        this.mimeType = mimeType;
        this.text = text;
    }
}
