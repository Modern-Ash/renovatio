package org.shark.renovatio.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.shark.renovatio.llm.decision.ArchitectureSuggestionCoordinator;
import org.shark.renovatio.llm.decision.ArchitectureSuggestionGateway;
import org.shark.renovatio.llm.decision.DecisionSuggestionService;
import org.shark.renovatio.llm.enrichment.AttributionException;
import org.shark.renovatio.llm.residual.ControlFlowPlanGate;
import org.shark.renovatio.llm.residual.ResidualAnnotationAssembler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
    "org.shark.renovatio.core",
    "org.shark.renovatio.shared",
    "org.shark.renovatio.provider.java",
    "org.shark.renovatio.provider.cobol",
    "org.shark.renovatio.api"
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
