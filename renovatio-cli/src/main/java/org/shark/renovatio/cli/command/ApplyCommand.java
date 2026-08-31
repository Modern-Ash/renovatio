package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.WorkspaceStateStore;
import org.shark.renovatio.cli.WorkspaceStateStore.PlanDescriptor;
import org.shark.renovatio.cli.WorkspaceStateStore.RunDescriptor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Command(name = "apply", description = "Apply a migration plan produced by 'renovatio plan'.")
public final class ApplyCommand extends AbstractCoreCommand {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git",
            ".renovatio",
            "target",
            "generated-java-stubs",
            "generated-decomposed"
    );

    @Parameters(index = "0", paramLabel = "<planId>", description = "Plan id printed by 'renovatio plan'.")
    String planId;

    @Option(names = "--dry-run", negatable = true, defaultValue = "true",
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

        Path planWorkspace = Path.of(plan.workspacePath());
        Path executionWorkspace = planWorkspace;
        Path tempWorkspace = null;

        try {
            if (dryRun || hasOut()) {
                tempWorkspace = Files.createTempDirectory("renovatio-cli-apply-");
                copyWorkspace(planWorkspace, tempWorkspace);
                executionWorkspace = tempWorkspace;
            }

            MigrationChain.Step applied = chain.apply(planned.engineId(), executionWorkspace.toString(), dryRun, out);
            if (!applied.ok()) {
                return output().render(applied.result(), r -> { });
            }

            if (!dryRun && hasOut()) {
                copyDirectory(executionWorkspace.resolve("generated-java-stubs"), Path.of(out).toAbsolutePath().normalize());
            }

            String cliRunId = UUID.randomUUID().toString();
            store.saveRun(new RunDescriptor(cliRunId, planId, plan.workspacePath(), dryRun, out,
                    WorkspaceStateStore.now()));

            Map<String, Object> view = new LinkedHashMap<>(applied.result());
            view.put("runId", cliRunId);
            view.put("engineRunId", applied.engineId());
            view.put("planId", planId);
            if (!dryRun && hasOut()) {
                view.put("outputDir", Path.of(out).toAbsolutePath().normalize().toString());
            }
            return output().render(view, r -> {
                System.out.println("runId: " + cliRunId);
                System.out.println("dryRun: " + dryRun);
                Object changes = r.get("changes");
                if (changes instanceof Map<?, ?> c) {
                    c.forEach((k, v) -> System.out.println("  " + k + ": " + v));
                }
                if (!dryRun && hasOut()) {
                    System.out.println("output: " + Path.of(out).toAbsolutePath().normalize());
                }
                System.out.println();
                System.out.println("next: renovatio diff " + cliRunId);
            });
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
            return 1;
        } finally {
            if (tempWorkspace != null) {
                deleteRecursively(tempWorkspace);
            }
        }
    }

    private boolean hasOut() {
        return out != null && !out.isBlank();
    }

    private static void copyWorkspace(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (shouldSkip(source.relativize(dir))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path relative = source.relativize(dir);
                if (!relative.toString().isEmpty()) {
                    Files.createDirectories(target.resolve(relative));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                if (!shouldSkip(relative)) {
                    Path destination = target.resolve(relative);
                    Path parent = destination.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(file, destination,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        Files.createDirectories(target);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                if (!relative.toString().isEmpty()) {
                    Files.createDirectories(target.resolve(relative));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Path destination = target.resolve(relative);
                Path parent = destination.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.copy(file, destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean shouldSkip(Path relative) {
        return relative != null
                && relative.getNameCount() > 0
                && EXCLUDED_DIRECTORIES.contains(relative.getName(0).toString());
    }

    private static void deleteRecursively(Path root) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup. Temporary workspaces are safe to leave behind if deletion fails.
        }
    }
}
