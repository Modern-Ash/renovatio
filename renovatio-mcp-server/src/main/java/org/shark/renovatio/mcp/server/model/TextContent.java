package org.shark.renovatio.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Simple MCP content part representing a piece of text that can be returned to the client.
 */
@Data
@NoArgsConstructor
public class TextContent {
    public static final String TEXT_TYPE = "text";

    private String type;
    private String text;

    @JsonCreator
    public TextContent(@JsonProperty("type") String type, @JsonProperty("text") String text) {
        this.type = type != null ? type : TEXT_TYPE;
        this.text = Objects.requireNonNullElse(text, "");
    }

    public TextContent(String text) {
        this.type = TEXT_TYPE;
        this.text = Objects.requireNonNullElse(text, "");
    }
}
