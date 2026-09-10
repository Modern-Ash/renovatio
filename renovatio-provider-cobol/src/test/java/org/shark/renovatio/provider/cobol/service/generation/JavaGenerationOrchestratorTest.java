package org.shark.renovatio.provider.cobol.service.generation;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavaGenerationOrchestratorTest {

    @Test
    void productionBridgeDelegatesDefaultGeneration() {
        JavaGenerationService generationService = mock(JavaGenerationService.class);
        JavaGenerationOrchestrator orchestrator = new JavaGenerationOrchestrator(generationService);
        NqlQuery query = new NqlQuery();
        Workspace workspace = new Workspace("project", "/tmp/workspace", "main");
        StubResult expected = new StubResult(true, "generated");
        when(generationService.generateInterfaceStubs(query, workspace)).thenReturn(expected);

        StubResult actual = orchestrator.generateInterfaceStubs(query, workspace);

        assertThat(actual).isSameAs(expected);
        verify(generationService).generateInterfaceStubs(query, workspace);
    }
}
