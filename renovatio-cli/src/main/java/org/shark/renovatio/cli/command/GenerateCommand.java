package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.ReusableProjectStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Command(name = "generate", mixinStandardHelpOptions = true,
        description = "Generate target artifacts using an explicit migration profile.")
public final class GenerateCommand extends AbstractCoreCommand {

    @Parameters(index = "0", paramLabel = "<path>", description = "Workspace containing COBOL sources.")
    Path path;

    @Option(names = "--profile", required = true, description = "Migration profile in JSON, YAML, or YML format.")
    Path profileFile;

    @Option(names = "--out", description = "Output directory (relative paths resolve beneath the workspace).")
    Path out;

    @Override
    public Integer call() {
        Path workspace = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(workspace)) {
            return failure("workspace directory not found: " + workspace);
        }

        MigrationProfile profile;
        try {
            profile = readProfile(profileFile);
        } catch (IOException | IllegalArgumentException exception) {
            return failure("invalid migration profile: " + exception.getMessage());
        }

        Path selectedOutput = null;
        if (out != null) {
            selectedOutput = out.isAbsolute() ? out.normalize() : workspace.resolve(out).normalize();
            if (!out.isAbsolute() && !selectedOutput.startsWith(workspace)) {
                return failure("relative output directory must stay within the workspace");
            }
        }

        ReusableProjectStore store = new ReusableProjectStore(workspace);
        store.profile(profile);
        MigrationProfile.Language target;
        try {
            target = store.effectiveProfile().profile().target().language();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure("cannot resolve migration profile: " + exception.getMessage());
        }
        Path output = selectedOutput == null
                ? workspace.resolve(target == MigrationProfile.Language.JAVA
                ? "generated-java-stubs" : "generated-" + target.name().toLowerCase(Locale.ROOT) + "-stubs")
                : selectedOutput;
        Map<String, Object> arguments = args();
        arguments.put("workspacePath", workspace.toString());
        arguments.put("projectId", workspace.toString());
        arguments.put("outputDir", output.toString());

        Map<String, Object> routed = route("cobol.stubs", arguments);
        Map<String, Object> view = new LinkedHashMap<>(routed);
        if (Boolean.TRUE.equals(view.get("success"))) {
            view.put("outputDir", output.toString());
        }
        return output().render(view, result -> {
            output().line("target: " + result.getOrDefault("targetLanguage", "unknown"));
            output().line("artifacts: " + result.getOrDefault("artifactCount", 0));
            output().line("output: " + result.get("outputDir"));
        });
    }

    private Integer failure(String message) {
        return output().render(Map.of("success", false, "message", message), ignored -> { });
    }

    private static MigrationProfile readProfile(Path source) throws IOException {
        String content = Files.readString(source);
        String fileName = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return MigrationProfiles.readYaml(content);
        }
        if (fileName.endsWith(".json")) {
            return MigrationProfiles.readJson(content);
        }
        throw new IllegalArgumentException("profile must use a .json, .yaml, or .yml extension");
    }
}
