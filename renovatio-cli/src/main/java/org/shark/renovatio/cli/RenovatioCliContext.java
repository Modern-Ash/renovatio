package org.shark.renovatio.cli;

import org.shark.renovatio.core.service.LanguageProviderRegistry;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Owns a single headless Spring {@link ConfigurableApplicationContext} for the life of the CLI
 * process. Building the context is relatively expensive (a few seconds), so it is created lazily
 * and reused by every subcommand.
 */
public final class RenovatioCliContext implements AutoCloseable {

    private static RenovatioCliContext shared;

    private final ConfigurableApplicationContext context;

    private RenovatioCliContext() {
        this.context = new SpringApplicationBuilder(RenovatioCliConfiguration.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .properties("logging.level.root=WARN", "spring.main.log-startup-info=false")
                .run();
    }

    /** Returns the process-wide context, building it on first use. */
    public static synchronized RenovatioCliContext shared() {
        if (shared == null) {
            shared = new RenovatioCliContext();
            Runtime.getRuntime().addShutdownHook(new Thread(shared::close, "renovatio-cli-context-close"));
        }
        return shared;
    }

    public LanguageProviderRegistry registry() {
        return context.getBean(LanguageProviderRegistry.class);
    }

    public <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    public synchronized void close() {
        if (context.isActive()) {
            context.close();
        }
    }
}
