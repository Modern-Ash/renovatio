package org.shark.renovatio.api.config;

import org.shark.renovatio.shared.security.WorkspaceRootPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

    @Bean
    String deploymentModeGuard(
            @Value("${renovatio.security.deployment-mode:local-only}") String deploymentMode,
            @Value("${server.address:127.0.0.1}") String serverAddress) {
        String mode = deploymentMode.trim().toLowerCase(Locale.ROOT);
        if (!mode.equals("local-only") && !mode.equals("remote")) {
            throw new IllegalStateException("Unsupported renovatio.security.deployment-mode: " + deploymentMode);
        }
        if (mode.equals("local-only") && !isLoopback(serverAddress)) {
            throw new IllegalStateException("local-only deployment mode requires a loopback server.address");
        }
        if (mode.equals("remote")) {
            throw new IllegalStateException("remote deployment mode requires OIDC/auth hardening before startup");
        }
        return mode;
    }

    private boolean isLoopback(String serverAddress) {
        if (serverAddress == null || serverAddress.isBlank()) {
            return false;
        }
        String value = serverAddress.trim();
        if ("localhost".equalsIgnoreCase(value)) {
            return true;
        }
        try {
            return InetAddress.getByName(value).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
