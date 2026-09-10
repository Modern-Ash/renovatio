package org.shark.renovatio.application.spi;

import java.util.Map;

/**
 * Compatibility entry point used while legacy providers are moved behind typed application ports.
 * Transport adapters must call this boundary instead of provider registries or migration services.
 */
@FunctionalInterface
public interface ApplicationCommandBus {
    Map<String, Object> execute(String capability, Map<String, Object> arguments);
}
