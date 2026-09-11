package org.shark.renovatio.application;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.application.model.ApplicationModel.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRenovatioApplicationTest {
    @Test void exposesAllNineUseCasesAndKeepsPlanningAndPreviewReadOnly() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        assertNotNull(app.reviewDomain(new ReviewDomain("project")));
        MigrationPlan first = app.plan(new Plan("project"));
        MigrationPlan second = app.plan(new Plan("project"));
        assertEquals(first.id(), second.id());
        ArtifactManifest preview = app.preview(new Preview("project", first.id()));
        assertTrue(kit.artifacts.workspace.isEmpty());
        assertTrue(app.validate(new Validate("project", preview.id())).valid());
        assertTrue(app.exportEvidence(new ExportEvidence("project")).isEmpty());
    }

    @Test void previewIsDeterministicAndApplyUsesExactlyItsManifest() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        MigrationPlan plan = app.plan(new Plan("project"));
        ArtifactManifest first = app.preview(new Preview("project", plan.id()));
        ArtifactManifest second = app.preview(new Preview("project", plan.id()));
        assertEquals(first.id(), second.id());
        ChangeSet applied = app.apply(new Apply("project", first.id(), first.sourceHash(), "apply-1"));
        assertEquals(first.id(), applied.manifestId());
        assertEquals(ChangeState.APPLIED, applied.state());
        assertArrayEquals(first.artifacts().get("generated.txt"), kit.artifacts.workspace.get("generated.txt"));
    }

    @Test void previewRunsBatchOrchestrationThroughTheCanonicalApplicationBoundary() {
        var kit = new ApplicationContractTestKit(); var app = kit.application();
        app.createProject(new CreateProject("project", "Project", "create-1", Map.of()));
        app.analyzeProject(new AnalyzeProject("project"));
        app.resolveDecisions(new ResolveDecisions("project", "decisions-1",
                Map.of("style", "hexagonal", "batch.target", "SPRING_BATCH")));
        MigrationPlan plan = app.plan(new Plan("project"));

        ArtifactManifest manifest = app.preview(new Preview("project", plan.id()));

        assertEquals(1, kit.batchCalls);
        assertTrue(manifest.artifacts().containsKey("generated.txt"));
        assertEquals("batch:SPRING_BATCH:[generated.txt]",
                new String(manifest.artifacts().get("batch/orchestration.txt"), StandardCharsets.UTF_8));
    }

    @Test void previewRejectsDuplicateTargetAndBatchArtifactPaths() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        kit.batchCollides = true;
        MigrationPlan plan = app.plan(new Plan("project"));

        assertThrows(ApplicationFailure.ValidationFailed.class,
                () -> app.preview(new Preview("project", plan.id())));
    }

    @Test void applyReplayDoesNotRepeatWritesOrGitAndConflictingKeyFails() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        MigrationPlan plan = app.plan(new Plan("project"));
        ArtifactManifest manifest = app.preview(new Preview("project", plan.id()));
        Apply command = new Apply("project", manifest.id(), manifest.sourceHash(), "apply-1");
        ChangeSet first = app.apply(command); ChangeSet replay = app.apply(command);
        assertSame(first, replay); assertEquals(1, kit.git.checkpoints);
        assertThrows(ApplicationFailure.IdempotencyConflict.class,
                () -> app.apply(new Apply("project", manifest.id(), "different", "apply-1")));
    }

    @Test void rejectsStaleSourceBeforeWriting() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        MigrationPlan plan = app.plan(new Plan("project"));
        ArtifactManifest manifest = app.preview(new Preview("project", plan.id()));
        kit.analyzer.source = ApplicationContractTestKit.bytes("source.cob", "changed");
        assertThrows(ApplicationFailure.StaleSource.class,
                () -> app.apply(new Apply("project", manifest.id(), manifest.sourceHash(), "apply-1")));
        assertTrue(kit.artifacts.workspace.isEmpty()); assertEquals(0, kit.git.checkpoints);
    }

    @Test void writeFailureRestoresPreimageAndRecordsRevertedChangeSet() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        kit.artifacts.workspace = ApplicationContractTestKit.bytes("existing.txt", "before");
        MigrationPlan plan = app.plan(new Plan("project"));
        ArtifactManifest manifest = app.preview(new Preview("project", plan.id()));
        kit.artifacts.failReplace = true;
        assertThrows(ApplicationFailure.ApplyReverted.class,
                () -> app.apply(new Apply("project", manifest.id(), manifest.sourceHash(), "apply-1")));
        assertEquals("before", new String(kit.artifacts.workspace.get("existing.txt"), StandardCharsets.UTF_8));
        assertEquals(ChangeState.REVERTED, kit.artifacts.history.get(kit.artifacts.history.size() - 1).state());
    }

    @Test void gitFailureRestoresWorkspace() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        kit.artifacts.workspace = ApplicationContractTestKit.bytes("existing.txt", "before"); kit.git.fail = true;
        MigrationPlan plan = app.plan(new Plan("project")); ArtifactManifest manifest = app.preview(new Preview("project", plan.id()));
        assertThrows(ApplicationFailure.ApplyReverted.class,
                () -> app.apply(new Apply("project", manifest.id(), manifest.sourceHash(), "apply-1")));
        assertEquals("before", new String(kit.artifacts.workspace.get("existing.txt"), StandardCharsets.UTF_8));
    }

    @Test void idempotencyPersistenceFailureCompensatesCompletedApplyEffects() {
        var kit = new ApplicationContractTestKit(); var app = kit.application(); kit.seed(app);
        kit.artifacts.workspace = ApplicationContractTestKit.bytes("existing.txt", "before");
        MigrationPlan plan = app.plan(new Plan("project"));
        ArtifactManifest manifest = app.preview(new Preview("project", plan.id()));
        kit.idempotency.failSave = true;
        assertThrows(ApplicationFailure.ApplyReverted.class,
                () -> app.apply(new Apply("project", manifest.id(), manifest.sourceHash(), "apply-fails")));
        assertEquals("before", new String(kit.artifacts.workspace.get("existing.txt"), StandardCharsets.UTF_8));
        assertEquals(1, kit.git.compensations);
        assertTrue(kit.idempotency.find("project", "apply", "apply-fails").isEmpty());
        assertEquals(ChangeState.REVERTED, kit.artifacts.history.get(kit.artifacts.history.size() - 1).state());
    }

    @Test void valuesDefensivelyCopyArtifactBytes() {
        byte[] bytes = "safe".getBytes(StandardCharsets.UTF_8);
        SourceSnapshot snapshot = new SourceSnapshot("project", null, Map.of("file", bytes));
        bytes[0] = 'X'; assertEquals("safe", new String(snapshot.files().get("file"), StandardCharsets.UTF_8));
        snapshot.files().get("file")[0] = 'Y';
        assertEquals("safe", new String(snapshot.files().get("file"), StandardCharsets.UTF_8));
    }
}
