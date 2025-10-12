package org.shark.renovatio.mcp.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "MCP Server capabilities")
@Data
public class McpCapabilities {
    @Schema(description = "Tools capability")
    private ToolsCapability tools;

    @Schema(description = "Prompts capability")
    private PromptsCapability prompts;

    @Schema(description = "Resources capability")
    private ResourcesCapability resources;

    public McpCapabilities() {
        this.tools = new ToolsCapability();
        this.prompts = new PromptsCapability();
        this.resources = new ResourcesCapability();
    }

    @Data
    public static class ToolsCapability {
        @Schema(description = "Whether server supports listing tools")
        private boolean listChanged = true;
    }

    @Data
    public static class PromptsCapability {
        @Schema(description = "Whether server supports listing prompts")
        private boolean listChanged = true;
    }

    @Data
    public static class ResourcesCapability {
        @Schema(description = "Whether server supports listing resources")
        private boolean listChanged = true;
    }
}