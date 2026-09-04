package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.ReusableProjectStore;
import org.shark.renovatio.profile.MigrationProfiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
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
        String workspacePath = absolute(path);
        args.put("workspacePath", workspacePath);
        args.put("projectId", workspacePath);
        putIfPresent(args, "scope", scope);
        putIfPresent(args, "dialect", dialect);

        Map<String, Object> result = route("cobol.analyze", args);
        if (Boolean.TRUE.equals(result.get("success"))) {
            Map<String, Object> semanticProjection = new LinkedHashMap<>();
            for (String key : new String[] {"data", "ast", "symbols", "dependencies"}) {
                if (result.get(key) != null) semanticProjection.put(key, result.get(key));
            }
            String semanticIrHash = MigrationProfiles.sha256(MigrationProfiles.canonical(semanticProjection));
            new ReusableProjectStore(path).reconcileAnalysis(semanticIrHash, Instant.now());
        }
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
