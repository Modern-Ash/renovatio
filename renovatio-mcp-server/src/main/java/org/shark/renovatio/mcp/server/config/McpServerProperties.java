package org.shark.renovatio.mcp.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the MCP server.
 */
@Component
@ConfigurationProperties(prefix = "renovatio")
public class McpServerProperties {

    private String defaultLanguage;

    /**
     * Get the default language filter for MCP tools.
     * Can be "java", "cobol", or null/empty for all languages.
     *
     * @return the default language, or null if not set
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Set the default language filter for MCP tools.
     *
     * @param defaultLanguage the language to filter by (java, cobol, or empty)
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
