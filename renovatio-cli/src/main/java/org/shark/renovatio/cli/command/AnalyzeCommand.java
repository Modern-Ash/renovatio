package org.shark.renovatio.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.Map;

@Command(name = "analyze", description = "Analyze the COBOL programs in a workspace.")
public final class AnalyzeCommand extends AbstractCoreCommand {

    @Parameters(index = "0", paramLabel = "<path>", description = "Workspace directory containing COBOL sources.")
    Path path;

    @Option(names = "--scope", description = "Glob restricting the files considered, e.g. '**/*.cbl'.")
    String scope;

    @Option(names = "--dialect", description = "COBOL dialect hint (IBM, GNU, MF).")
    String dialect;

    @Override
    public Integer call() {
        Map<String, Object> args = args();
        args.put("workspacePath", absolute(path));
        putIfPresent(args, "scope", scope);
        putIfPresent(args, "dialect", dialect);

        Map<String, Object> result = route("cobol.analyze", args);
        return output().render(result, r -> {
            Object metrics = r.get("metrics");
            System.out.println("workspace: " + absolute(path));
            System.out.println(r.getOrDefault("summary", r.getOrDefault("message", "analysis complete")));
            if (metrics instanceof Map<?, ?> m && !m.isEmpty()) {
                System.out.println("metrics:");
                m.forEach((k, v) -> System.out.println("  " + k + ": " + v));
            }
            Object files = r.get("analyzedFiles");
            if (files instanceof java.util.Collection<?> c && !c.isEmpty()) {
                System.out.println("analyzed files: " + c.size());
            }
        });
    }
}
