package org.shark.renovatio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal Spring configuration for the headless CLI context. Scans the core engine and the
 * language providers so every {@code LanguageProvider} bean is registered exactly as it is for
 * the MCP server, without starting any web server.
 */
@Configuration
@ComponentScan(basePackages = {
        "org.shark.renovatio.core",
        "org.shark.renovatio.shared",
        "org.shark.renovatio.provider.cobol",
        "org.shark.renovatio.provider.java"
})
public class RenovatioCliConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
