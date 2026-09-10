package org.shark.renovatio.provider.cobol.service.generation;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JavaGenerationOrchestratorTest {

    @Test
    void productionBridgeDelegatesDefaultGeneration() {
        NqlQuery query = new NqlQuery();
        Workspace workspace = new Workspace("project", "/tmp/workspace", "main");
        StubResult expected = new StubResult(true, "generated");
        AtomicBoolean called = new AtomicBoolean();
        JavaGenerationService generationService = new JavaGenerationService(null, null, null, null) {
            @Override
            public StubResult generateInterfaceStubs(NqlQuery actualQuery, Workspace actualWorkspace) {
                called.set(actualQuery == query && actualWorkspace == workspace);
                return expected;
            }
        };
        JavaGenerationOrchestrator orchestrator = new JavaGenerationOrchestrator(generationService);

        StubResult actual = orchestrator.generateInterfaceStubs(query, workspace);

        assertThat(actual).isSameAs(expected);
        assertThat(called).isTrue();
    }
}
