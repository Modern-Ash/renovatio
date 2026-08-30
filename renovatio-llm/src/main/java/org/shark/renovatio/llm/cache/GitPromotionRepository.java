package org.shark.renovatio.llm.cache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** Git-backed immutable promotion history reader. */
public final class GitPromotionRepository implements PromotionRepository {
    private final Path repository;

    public GitPromotionRepository(Path repository) {
        this.repository = repository.toAbsolutePath().normalize();
    }

    @Override public String head() { return text("rev-parse", "HEAD").trim(); }

    @Override public boolean isAncestor(String ancestor, String descendant) {
        return exitCode("merge-base", "--is-ancestor", ancestor, descendant) == 0;
    }

    @Override public byte[] read(String revision, String repositoryPath) {
        return successful("show", revision + ":" + repositoryPath);
    }

    @Override public List<String> changedPaths(String revision) {
        String value = new String(successful("diff-tree", "--root", "--no-commit-id", "--name-only",
                "-r", revision), StandardCharsets.UTF_8);
        return value.lines().filter(line -> !line.isBlank()).toList();
    }

    @Override public String commitIntroducing(String repositoryPath) {
        String commits = text("log", "--diff-filter=A", "--format=%H", "--", repositoryPath);
        return commits.lines().filter(line -> !line.isBlank()).findFirst()
                .orElseThrow(() -> new IllegalStateException("CACHE_PROMOTION_MANIFEST_COMMIT_MISSING"));
    }

    private String text(String... arguments) {
        return new String(successful(arguments), StandardCharsets.UTF_8);
    }

    private byte[] successful(String... arguments) {
        ProcessResult result = run(arguments);
        if (result.exitCode() != 0) throw new IllegalStateException("CACHE_PROMOTION_GIT_READ_FAILED");
        return result.stdout();
    }

    private int exitCode(String... arguments) { return run(arguments).exitCode(); }

    private ProcessResult run(String... arguments) {
        try {
            String[] command = new String[arguments.length + 1];
            command[0] = "git";
            System.arraycopy(arguments, 0, command, 1, arguments.length);
            Process process = new ProcessBuilder(command).directory(repository.toFile()).start();
            byte[] stdout = process.getInputStream().readAllBytes();
            process.getErrorStream().readAllBytes();
            return new ProcessResult(process.waitFor(), stdout);
        } catch (IOException exception) {
            throw new IllegalStateException("CACHE_PROMOTION_GIT_READ_FAILED", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CACHE_PROMOTION_GIT_INTERRUPTED", exception);
        }
    }

    private record ProcessResult(int exitCode, byte[] stdout) { }
}
