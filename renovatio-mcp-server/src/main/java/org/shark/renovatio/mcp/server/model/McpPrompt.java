package org.shark.renovatio.mcp.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "MCP Prompt definition")
@Data
public class McpPrompt {
    @Schema(description = "Prompt name")
    private String name;

    @Schema(description = "Prompt description")
    private String description;

    @Schema(description = "Prompt messages")
    private List<Message> messages;

    public McpPrompt() {
    }

    public McpPrompt(String name, String description, List<Message> messages) {
        this.name = name;
        this.description = description;
        this.messages = messages;
    }

    @Schema(description = "Prompt message")
    @Data
    public static class Message {
        @Schema(description = "Message role", example = "user")
        private String role;

        @Schema(description = "Message content")
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
