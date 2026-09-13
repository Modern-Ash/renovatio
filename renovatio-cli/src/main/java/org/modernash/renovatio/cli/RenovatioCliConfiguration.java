package org.modernash.renovatio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modernash.renovatio.profile.EffectiveProfileResolver;
import org.modernash.renovatio.profile.MigrationProfiles;
import org.modernash.renovatio.emitter.node.config.NodeEmitterAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;

/**
 * Minimal Spring configuration for the headless CLI context. Scans the core engine and the
 * language providers so every {@code LanguageProvider} bean is registered exactly as it is for
 * the MCP server, without starting any web server.
 */
@Configuration
@Import(NodeEmitterAutoConfiguration.class)
@ComponentScan(basePackages = {
        "org.modernash.renovatio.core",
        "org.modernash.renovatio.shared",
        "org.modernash.renovatio.provider.cobol",
        "org.modernash.renovatio.provider.java"
})
public class RenovatioCliConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public EffectiveProfileResolver localEffectiveProfileResolver() {
        return projectId -> projectId == null || projectId.isBlank() || "default".equals(projectId)
                ? MigrationProfiles.effective(MigrationProfiles.emptyOverlay(), java.util.Map.of(),
                java.util.Map.of(), java.util.List.of())
                : new ReusableProjectStore(Path.of(projectId)).effectiveProfile();
    }
}
