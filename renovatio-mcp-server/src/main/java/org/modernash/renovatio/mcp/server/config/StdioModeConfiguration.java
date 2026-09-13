package org.modernash.renovatio.mcp.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Configuration for MCP stdio mode that excludes web-related components.
 */
@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "none")
@ComponentScan(
        basePackages = {
                "org.modernash.renovatio.mcp.server",
                "org.modernash.renovatio.core.service",
                "org.modernash.renovatio.core.application",
                "org.modernash.renovatio.shared"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.modernash\\.renovatio\\.core\\.api\\..*")
        }
)
public class StdioModeConfiguration {
}
