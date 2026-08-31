package org.shark.renovatio.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.Map;

@Command(name = "metrics", description = "Report size and complexity metrics for a COBOL workspace.")
public final class MetricsCommand extends AbstractCoreCommand {

    @Parameters(index = "0", paramLabel = "<path>", description = "Workspace directory containing COBOL sources.")
    Path path;

    @Option(names = "--scope", description = "Glob restricting the files considered, e.g. '**/*.cbl'.")
    String scope;

    @Override
    public Integer call() {
        Map<String, Object> args = args();
        args.put("workspacePath", absolute(path));
        putIfPresent(args, "scope", scope);

        Map<String, Object> result = route("cobol.metrics", args);
        return output().render(result, r -> {
            System.out.println("workspace: " + absolute(path));
            Object metrics = r.get("metrics");
            if (metrics instanceof Map<?, ?> m && !m.isEmpty()) {
                m.forEach((k, v) -> System.out.printf("  %-28s %s%n", k, v));
            } else {
                System.out.println("  " + r.getOrDefault("message", "no metrics reported"));
            }
            Object details = r.get("details");
            if (details instanceof Map<?, ?> d && !d.isEmpty()) {
                System.out.println("details:");
                d.forEach((k, v) -> System.out.printf("  %-28s %s%n", k, v));
            }
        });
    }
}
