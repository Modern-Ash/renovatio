package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.BaseLanguageProvider;
import org.shark.renovatio.shared.spi.LanguageProvider;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests to verify that duplicate providers are detected at startup.
 * This ensures single-authority per language/capability.
 */
class DuplicateProviderDetectionTest {

    @Test
    void shouldRejectProvidersWithOverlappingCapabilitiesForSameLanguage() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();

        // Create two providers for the same language
        LanguageProvider provider1 = new TestLanguageProvider("java");
        LanguageProvider provider2 = new TestLanguageProvider("java");

        registry.registerProvider(provider1);
        assertThrows(IllegalStateException.class, () -> registry.registerProvider(provider2));
    }

    @Test
    void shouldRejectDifferentProviderTypesWithOverlappingCapabilities() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();

        // Create two different provider types for the same language
        LanguageProvider provider1 = new TestLanguageProvider("java");
        LanguageProvider provider2 = new AnotherTestLanguageProvider("java");

        registry.registerProvider(provider1);
        assertThrows(IllegalStateException.class, () -> registry.registerProvider(provider2));
    }

    @Test
    void shouldNotRegisterSameInstanceTwice() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();

        // Create a provider
        LanguageProvider provider = new TestLanguageProvider("java");

        // Register the same instance twice - should only register once
        registry.registerProvider(provider);
        registry.registerProvider(provider);

        // Verify only one is registered
        assertEquals(1, registry.getAllProviders().size());
    }

    /**
     * Test language provider implementation.
     */
    private static class TestLanguageProvider extends BaseLanguageProvider {
        private final String language;

        TestLanguageProvider(String language) {
            this.language = language;
        }

        @Override
        public String language() {
            return language;
        }

        @Override
        public Set<LanguageProvider.Capabilities> capabilities() {
            return EnumSet.of(LanguageProvider.Capabilities.ANALYZE);
        }

        @Override
        public AnalyzeResult analyze(NqlQuery query, Workspace workspace) {
            return new AnalyzeResult(true, "test");
        }

        @Override
        public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) {
            return new PlanResult(true, "test");
        }

        @Override
        public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) {
            return new ApplyResult(true, "test");
        }

        @Override
        public DiffResult diff(String runId, Workspace workspace) {
            return new DiffResult(true, "test");
        }

        @Override
        public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) {
            return Optional.empty();
        }

        @Override
        public MetricsResult metrics(Scope scope, Workspace workspace) {
            return new MetricsResult(true, "test");
        }

        @Override
        public List<Tool> getTools() {
            return List.of();
        }
    }

    /**
     * Another test language provider implementation.
     */
    private static class AnotherTestLanguageProvider extends BaseLanguageProvider {
        private final String language;

        AnotherTestLanguageProvider(String language) {
            this.language = language;
        }

        @Override
        public String language() {
            return language;
        }

        @Override
        public Set<LanguageProvider.Capabilities> capabilities() {
            return EnumSet.of(LanguageProvider.Capabilities.ANALYZE);
        }

        @Override
        public AnalyzeResult analyze(NqlQuery query, Workspace workspace) {
            return new AnalyzeResult(true, "test");
        }

        @Override
        public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) {
            return new PlanResult(true, "test");
        }

        @Override
        public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) {
            return new ApplyResult(true, "test");
        }

        @Override
        public DiffResult diff(String runId, Workspace workspace) {
            return new DiffResult(true, "test");
        }

        @Override
        public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) {
            return Optional.empty();
        }

        @Override
        public MetricsResult metrics(Scope scope, Workspace workspace) {
            return new MetricsResult(true, "test");
        }

        @Override
        public List<Tool> getTools() {
            return List.of();
        }
    }
}
