package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.WorkspaceStateStore;
import org.shark.renovatio.cli.WorkspaceStateStore.RunDescriptor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Command(name = "diff", description = "Show the diff produced by a migration run.")
public final class DiffCommand extends AbstractCoreCommand {

    @Parameters(index = "0", paramLabel = "<runId>", description = "Run id printed by 'renovatio apply'.")
    String runId;

    @Option(names = "--format", defaultValue = "unified",
            description = "Diff format: unified (default), semantic, or both.")
    String format;

    @Option(names = {"-w", "--workspace"}, defaultValue = ".",
            description = "Workspace the run was created for (default: current directory).")
    Path workspaceDir;

    @Override
    public Integer call() {
        Path workspace = workspaceDir.toAbsolutePath().normalize();
        WorkspaceStateStore store = new WorkspaceStateStore(workspace);
        Optional<RunDescriptor> descriptor = store.loadRun(runId);
        if (descriptor.isEmpty()) {
            System.err.println("error: unknown run id " + runId + " — run 'renovatio apply' first");
            return 1;
        }
        RunDescriptor run = descriptor.get();

        MigrationChain chain = new MigrationChain(this::route);
        MigrationChain.Step planned = chain.replayPlan(
                new WorkspaceStateStore.PlanDescriptor(
                        run.planId(), run.workspacePath(), null, null, null, null, run.createdAt()));
        if (!planned.ok()) {
            return output().render(planned.result(), r -> { });
        }
        MigrationChain.Step applied = chain.apply(planned.engineId(), run.workspacePath(), run.workspacePath(),
                true, null);
        if (!applied.ok()) {
            return output().render(applied.result(), r -> { });
        }

        Map<String, Object> diffResult = chain.diff(applied.engineId(), run.workspacePath());
        return output().render(diffResult, r -> {
            boolean showUnified = "unified".equals(format) || "both".equals(format);
            boolean showSemantic = "semantic".equals(format) || "both".equals(format);

            if (showUnified) {
                Object unified = r.get("diff");
                if (unified != null) {
                    System.out.println(unified);
                } else {
                    System.out.println("(no unified diff available)");
                }
            }
            if (showSemantic) {
                Object semantic = r.get("semanticDiff");
                if (semantic != null) {
                    if (showUnified) System.out.println();
                    System.out.println("semantic:");
                    System.out.println(semantic);
                }
            }
        });
    }
}
