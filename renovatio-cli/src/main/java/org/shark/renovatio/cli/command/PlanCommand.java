package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.WorkspaceStateStore;
import org.shark.renovatio.cli.WorkspaceStateStore.PlanDescriptor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Command(name = "plan", description = "Create a migration plan for a COBOL workspace.")
public final class PlanCommand extends AbstractCoreCommand {

    @Parameters(index = "0", paramLabel = "<path>", description = "Workspace directory containing COBOL sources.")
    Path path;

    @Option(names = "--scope", description = "Glob restricting the files considered.")
    String scope;

    @Option(names = "--nql", description = "Optional NQL query narrowing the plan.")
    String nql;

    @Option(names = "--strategy", description = "Migration strategy hint (incremental, full).")
    String strategy;

    @Option(names = "--framework", description = "Target framework hint (e.g. spring-boot).")
    String framework;

    @Override
    public Integer call() {
        String workspacePath = absolute(path);
        Map<String, Object> args = args();
        args.put("workspacePath", workspacePath);
        putIfPresent(args, "scope", scope);
        putIfPresent(args, "nql", nql);
        putIfPresent(args, "strategy", strategy);
        putIfPresent(args, "framework", framework);

        Map<String, Object> result = route("cobol.plan", args);
        if (!Boolean.TRUE.equals(result.get("success"))) {
            return output().render(result, r -> { });
        }

        String cliPlanId = UUID.randomUUID().toString();
        new WorkspaceStateStore(path).savePlan(new PlanDescriptor(
                cliPlanId, workspacePath, nql, scope, strategy, framework, WorkspaceStateStore.now()));

        Map<String, Object> view = new LinkedHashMap<>(result);
        view.put("planId", cliPlanId);
        view.put("enginePlanId", result.get("planId"));
        return output().render(view, r -> {
            System.out.println("planId: " + cliPlanId);
            Object content = r.get("planContent");
            if (content != null) {
                System.out.println(content);
            }
            Object steps = r.get("steps");
            if (steps instanceof Map<?, ?> s) {
                s.forEach((k, v) -> System.out.println("  " + k + ": " + v));
            }
            System.out.println();
            System.out.println("next: renovatio apply " + cliPlanId + " --dry-run");
        });
    }
}
