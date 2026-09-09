package org.shark.renovatio.core.service;

import org.shark.renovatio.application.spi.ApplicationCommandBus;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary AC-05 adapter that places provider routing behind the application boundary.
 * Provider retirement belongs to AC-06; transports must not inject the registry directly.
 */
@Service
public final class LegacyProviderApplicationAdapter implements ApplicationCommandBus {
    private final LanguageProviderRegistry providers;
    private final Map<String, String> plannedSourceHashes = new ConcurrentHashMap<>();

    public LegacyProviderApplicationAdapter(@Lazy LanguageProviderRegistry providers) {
        this.providers = Objects.requireNonNull(providers);
    }

    @Override
    public Map<String, Object> execute(String capability, Map<String, Object> arguments) {
        if (capability == null || capability.isBlank()) {
            throw new IllegalArgumentException("capability is required");
        }
        Map<String, Object> safeArguments = arguments == null ? new LinkedHashMap<>() : new LinkedHashMap<>(arguments);
        if (capability.endsWith(".apply")) {
            Map<String, Object> stale = rejectStalePlan(safeArguments);
            if (stale != null) return stale;
        }
        String sourceHash = capability.endsWith(".plan") ? sourceHash(safeArguments) : null;
        Map<String, Object> result = providers.routeToolCall(capability, safeArguments);
        if (sourceHash != null && Boolean.TRUE.equals(result.get("success")) && result.get("planId") != null) {
            plannedSourceHashes.put(result.get("planId").toString(), sourceHash);
        }
        return result;
    }

    private Map<String, Object> rejectStalePlan(Map<String, Object> arguments) {
        Object planId = arguments.get("planId");
        if (planId == null) return failure("planId is required");
        String plannedHash = plannedSourceHashes.get(planId.toString());
        if (plannedHash == null) return failure("source revision for plan not found: " + planId);
        if (!plannedHash.equals(sourceHash(arguments))) {
            return failure("stale plan rejected: workspace changed after planning");
        }
        return null;
    }

    private static Map<String, Object> failure(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("type", "apply");
        return result;
    }

    private static String sourceHash(Map<String, Object> arguments) {
        Object rawPath = arguments.get("workspacePath");
        if (rawPath == null || rawPath.toString().isBlank()) return "workspace:none";
        Path workspace = Path.of(rawPath.toString()).toAbsolutePath().normalize();
        Path output = outputPath(workspace, arguments.get("outputDir"));
        Path stateDirectory = workspace.resolve(".renovatio").normalize();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (!Files.exists(workspace)) return "workspace:missing:" + workspace;
            try (var paths = Files.walk(workspace)) {
                for (Path path : paths.filter(Files::isRegularFile).map(Path::normalize)
                        .filter(path -> output == null || !path.startsWith(output))
                        .filter(path -> !path.startsWith(stateDirectory))
                        .sorted(Comparator.comparing(Path::toString)).toList()) {
                    digest.update(workspace.relativize(path).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    digest.update(Files.readAllBytes(path));
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("cannot fingerprint workspace " + workspace, failure);
        }
    }

    private static Path outputPath(Path workspace, Object rawOutput) {
        if (rawOutput == null || rawOutput.toString().isBlank()) return null;
        Path output = Path.of(rawOutput.toString());
        return (output.isAbsolute() ? output : workspace.resolve(output)).toAbsolutePath().normalize();
    }
}
