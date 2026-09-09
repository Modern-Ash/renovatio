package org.shark.renovatio.core.service;

import org.shark.renovatio.application.spi.ApplicationCommandBus;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Temporary AC-05 adapter that places provider routing behind the application boundary.
 * Provider retirement belongs to AC-06; transports must not inject the registry directly.
 */
@Service
public final class LegacyProviderApplicationAdapter implements ApplicationCommandBus {
    private final LanguageProviderRegistry providers;

    public LegacyProviderApplicationAdapter(@Lazy LanguageProviderRegistry providers) {
        this.providers = Objects.requireNonNull(providers);
    }

    @Override
    public Map<String, Object> execute(String capability, Map<String, Object> arguments) {
        if (capability == null || capability.isBlank()) {
            throw new IllegalArgumentException("capability is required");
        }
        return providers.routeToolCall(capability,
                arguments == null ? new LinkedHashMap<>() : new LinkedHashMap<>(arguments));
    }
}
