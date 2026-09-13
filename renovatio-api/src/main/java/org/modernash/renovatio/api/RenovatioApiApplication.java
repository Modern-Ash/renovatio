package org.modernash.renovatio.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modernash.renovatio.provider.java.OpenRewriteRunner;
import org.modernash.renovatio.llm.decision.ArchitectureSuggestionCoordinator;
import org.modernash.renovatio.llm.decision.ArchitectureSuggestionGateway;
import org.modernash.renovatio.llm.decision.DecisionSuggestionService;
import org.modernash.renovatio.llm.enrichment.AttributionException;
import org.modernash.renovatio.llm.residual.ControlFlowPlanGate;
import org.modernash.renovatio.llm.residual.ResidualAnnotationAssembler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
    "org.modernash.renovatio.core",
    "org.modernash.renovatio.shared",
    "org.modernash.renovatio.provider.java",
    "org.modernash.renovatio.provider.cobol",
    "org.modernash.renovatio.llm",
    "org.modernash.renovatio.api"
})
@EnableAsync
public class RenovatioApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RenovatioApiApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public OpenRewriteRunner openRewriteRunner() {
        return new OpenRewriteRunner();
    }

    /**
     * Fail-closed default for API runs without a governed Agora attribution gateway.
     * Deployments may provide a governed runtime bean; deterministic high-confidence
     * F1 decisions do not invoke this miss path.
     */
    @Bean
    @ConditionalOnMissingBean(DecisionSuggestionService.SuggestionRuntime.class)
    public DecisionSuggestionService.SuggestionRuntime architectureSuggestionRuntime() {
        return (prompt, input, fallback) -> {
            throw new AttributionException(AttributionException.Stage.INIT);
        };
    }

    @Bean
    public ArchitectureSuggestionGateway architectureSuggestionCoordinator(
            DecisionSuggestionService.SuggestionRuntime runtime) {
        return new ArchitectureSuggestionCoordinator(new DecisionSuggestionService(runtime),
                new ControlFlowPlanGate(new ResidualAnnotationAssembler()));
    }
}
