package org.shark.renovatio.provider.cobol.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.architecture.ArtifactLayoutPlanner;
import org.shark.renovatio.shared.emission.TargetEmitterRegistry;
import org.shark.renovatio.profile.EffectiveProfileResolver;
import org.shark.renovatio.provider.cobol.CobolLanguageProvider;
import org.shark.renovatio.provider.cobol.service.*;
import org.shark.renovatio.provider.cobol.service.generation.JavaGenerationOrchestrator;
import org.shark.renovatio.provider.cobol.pipeline.CobolPipelineOrchestrator;
import org.shark.renovatio.provider.cobol.pipeline.EquivalenceCheckerImpl;
import org.shark.renovatio.provider.cobol.pipeline.MavenBuildServiceImpl;
import org.shark.renovatio.provider.cobol.pipeline.PipelineOrchestrator;
import org.shark.renovatio.architecture.java.JavaArchitectureLayoutPlanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

/**
 * Spring configuration for COBOL provider
 * Ensures all services are properly wired and available
 */
@Configuration
@ComponentScan(basePackages = "org.shark.renovatio.provider.cobol")
public class CobolProviderConfiguration {

    @Bean
    public CobolParsingService cobolParsingService(@Value("${renovatio.cobol.parser.dialect:IBM}") String dialect) {
        return new CobolParsingService(CobolParsingService.Dialect.fromString(dialect));
    }

    @Bean
    public JavaGenerationService javaGenerationService(
            CobolParsingService parsingService,
            TemplateCodeGenerationService templateCodeGenerationService,
            org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService intermediateModelService,
            org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler semanticTranspiler,
            ObjectMapper objectMapper,
            TargetEmitterRegistry targetEmitterRegistry,
            ObjectProvider<EffectiveProfileResolver> effectiveProfiles,
            ObjectProvider<ArtifactLayoutPlanner> externalLayoutPlanners) {
        var layoutPlanners = new ArrayList<ArtifactLayoutPlanner>();
        layoutPlanners.add(new JavaArchitectureLayoutPlanner());
        externalLayoutPlanners.orderedStream().forEach(layoutPlanners::add);
        return new JavaGenerationService(parsingService, templateCodeGenerationService,
                intermediateModelService, semanticTranspiler, objectMapper, true,
                targetEmitterRegistry, effectiveProfiles.getIfAvailable(), layoutPlanners);
    }

    @Bean
    public MigrationPlanService migrationPlanService(
            CobolParsingService parsingService,
            JavaGenerationService javaGenerationService) {
        return new MigrationPlanService(parsingService, javaGenerationService);
    }

    @Bean
    public JavaGenerationOrchestrator javaGenerationOrchestrator(JavaGenerationService generationService) {
        return new JavaGenerationOrchestrator(generationService);
    }

    @Bean
    public PipelineOrchestrator cobolReferencePipeline(
            CobolParsingService parsingService,
            JavaGenerationOrchestrator generationOrchestrator,
            org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler semanticTranspiler) {
        return new CobolPipelineOrchestrator(parsingService, generationOrchestrator, semanticTranspiler,
            new MavenBuildServiceImpl(), new EquivalenceCheckerImpl());
    }

    @Bean
    public IndexingService indexingService() {
        return new IndexingService();
    }

    @Bean
    public MetricsService metricsService() {
        return new MetricsService();
    }

    @Bean
    public CicsService cicsService(
            @Value("${renovatio.cics.mock:true}") boolean mock,
            @Value("${renovatio.cics.url:http://localhost:10080}") String url) {
        return mock ? new MockCicsService() : new RealCicsService(url);
    }

    @Bean
    public Db2MigrationService db2MigrationService(CobolParsingService parsingService) {
        return new Db2MigrationService(parsingService);
    }

    @Bean
    public ControlBreakDecompositionService controlBreakDecompositionService(
            org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService intermediateModelService,
            CobolParsingService parsingService) {
        return new ControlBreakDecompositionService(intermediateModelService, parsingService);
    }

    // Enable CobolLanguageProvider when COBOL provider is enabled via either legacy or new property name
    @Bean
    @ConditionalOnProperty(
            name = {"renovatio.providers.cobol.enabled", "renovatio.cobol.enabled"},
            havingValue = "true",
            matchIfMissing = true
    )
    public CobolLanguageProvider cobolLanguageProvider(
            CobolParsingService parsingService,
            JavaGenerationService javaGenerationService,
            JavaGenerationOrchestrator javaGenerationOrchestrator,
            MigrationPlanService migrationPlanService,
            IndexingService indexingService,
            MetricsService metricsService,
            TemplateCodeGenerationService templateCodeGenerationService,
            Db2MigrationService db2MigrationService,
            ControlBreakDecompositionService decompositionService) {
        return new CobolLanguageProvider(
                parsingService,
                javaGenerationService,
                javaGenerationOrchestrator,
                migrationPlanService,
                indexingService,
                metricsService,
                templateCodeGenerationService,
                db2MigrationService,
                decompositionService
        );
    }
}
