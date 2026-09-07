package org.shark.renovatio.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.dto.WorkbenchAssetDto;

class WorkbenchEquivalenceServiceTest {
    @Test
    void exposesOnlyWellFormedPersistedEquivalenceVerdicts(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve("equivalence.json"), """
                {"fixtureId":"balance","classification":"REGRESSION","reason":"invariant-delta","blocksRelease":true}
                """);
        Files.writeString(workspace.resolve("unrelated.json"), "{}\n");
        ProjectService projects = mock(ProjectService.class);
        WorkbenchProjectAdapterService assets = mock(WorkbenchProjectAdapterService.class);
        when(projects.getProject("project")).thenReturn(Optional.of(ProjectDto.builder().id("project")
                .workspacePath(workspace.toString()).build()));
        when(assets.list(workspace.toAbsolutePath().normalize(), false)).thenReturn(List.of(
                new WorkbenchAssetDto("equivalence.json", "equivalence.json", "Evidence", false),
                new WorkbenchAssetDto("unrelated.json", "unrelated.json", "Evidence", false),
                new WorkbenchAssetDto("PaymentService.java", "PaymentService.java", "Generated targets", false)));

        var summary = new WorkbenchEquivalenceService(projects, assets, new ObjectMapper()).summary("project");

        assertEquals(2, summary.evidence().size());
        assertEquals(1, summary.generatedTargets().size());
        assertEquals(1, summary.verdicts().size());
        assertEquals("balance", summary.verdicts().getFirst().fixtureId());
        assertEquals(true, summary.verdicts().getFirst().blocksRelease());
    }
}
