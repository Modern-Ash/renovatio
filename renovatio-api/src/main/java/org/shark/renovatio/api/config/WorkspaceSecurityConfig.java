package org.shark.renovatio.api.config;

import org.shark.renovatio.shared.security.WorkspaceRootPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Configuration
public class WorkspaceSecurityConfig {
    @Bean
    WorkspaceRootPolicy workspaceRootPolicy(
            @Value("${renovatio.security.workspace-roots:${user.home}/.renovatio/workspaces}") String roots) {
        List<Path> paths = Arrays.stream(roots.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .toList();
        return new WorkspaceRootPolicy(paths);
    }
}
