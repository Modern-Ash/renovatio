package org.shark.renovatio.mcp.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the MCP server.
 */
@Component
@ConfigurationProperties(prefix = "renovatio")
@Data
public class McpServerProperties {

    private String defaultLanguage;
}
