package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.WorkspaceStateStore;
import org.shark.renovatio.cli.WorkspaceStateStore.PlanDescriptor;
import org.shark.renovatio.cli.WorkspaceStateStore.RunDescriptor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Command(name = "apply", description = "Apply a migration plan produced by 'renovatio plan'.")
public final class ApplyCommand extends AbstractCoreCommand {

    @Parameters(index = "0", paramLabel = "<planId>", description = "Plan id printed by 'renovatio plan'.")
    String planId;

    @Option(names = {"--dry-run", "--no-dry-run"}, negatable = true, defaultValue = "true",
            description = "Preview without writing files (default: true).")
    boolean dryRun;

    @Option(names = "--out", description = "Output directory for generated Java (real apply only).")
    String out;

    @Option(names = {"-w", "--workspace"}, defaultValue = ".",
            description = "Workspace the plan was created for (default: current directory).")
    Path workspaceDir;

    @Override
    public Integer call() {
        Path workspace = workspaceDir.toAbsolutePath().normalize();
        WorkspaceStateStore store = new WorkspaceStateStore(workspace);
        Optional<PlanDescriptor> descriptor = store.loadPlan(planId);
        if (descriptor.isEmpty()) {
            System.err.println("error: unknown plan id " + planId + " — run 'renovatio plan' first");
            return 1;
        }
        PlanDescriptor plan = descriptor.get();

        MigrationChain chain = new MigrationChain(this::route);
        MigrationChain.Step planned = chain.replayPlan(plan);
        if (!planned.ok()) {
            return output().render(planned.result(), r -> { });
        }
        MigrationChain.Step applied = chain.apply(planned.engineId(), plan.workspacePath(), dryRun, out);
        if (!applied.ok()) {
            return output().render(applied.result(), r -> { });
        }

        String cliRunId = UUID.randomUUID().toString();
        store.saveRun(new RunDescriptor(cliRunId, planId, plan.workspacePath(), dryRun, out,
                WorkspaceStateStore.now()));

        Map<String, Object> view = new LinkedHashMap<>(applied.result());
        view.put("runId", cliRunId);
        view.put("engineRunId", applied.engineId());
        view.put("planId", planId);
        return output().render(view, r -> {
            System.out.println("runId: " + cliRunId);
            System.out.println("dryRun: " + dryRun);
            Object changes = r.get("changes");
            if (changes instanceof Map<?, ?> c) {
                c.forEach((k, v) -> System.out.println("  " + k + ": " + v));
            }
            if (!dryRun && out != null) {
                System.out.println("output: " + out);
            }
            System.out.println();
            System.out.println("next: renovatio diff " + cliRunId);
        });
    }
}
