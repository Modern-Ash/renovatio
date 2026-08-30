package org.shark.renovatio.llm.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** RepositoryTree implemented exclusively with git reads against HEAD. */
public final class GitHeadRepositoryTree implements RepositoryTree {
    private final Path repository;

    public GitHeadRepositoryTree(Path repository) {
        this.repository = repository.toAbsolutePath().normalize();
    }

    @Override public String revision() {
        return text("rev-parse", "HEAD").trim();
    }

    @Override public List<String> pathsUnder(String repositoryPrefix) {
        String output = text("ls-tree", "-r", "--name-only", "HEAD", "--", repositoryPrefix);
        return output.isBlank() ? List.of() : output.lines().filter(line -> !line.isBlank()).toList();
    }

    @Override public byte[] read(String repositoryPath) {
        return run("show", "HEAD:" + repositoryPath);
    }

    private String text(String... arguments) {
        return new String(run(arguments), java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] run(String... arguments) {
        try {
            String[] command = new String[arguments.length + 1];
            command[0] = "git";
            System.arraycopy(arguments, 0, command, 1, arguments.length);
            Process process = new ProcessBuilder(command).directory(repository.toFile()).start();
            byte[] stdout = process.getInputStream().readAllBytes();
            byte[] stderr = process.getErrorStream().readAllBytes();
            if (process.waitFor() != 0) {
                throw new IllegalStateException("Git tree read failed: "
                        + new String(stderr, java.nio.charset.StandardCharsets.UTF_8).trim());
            }
            return stdout;
        } catch (IOException exception) {
            throw new IllegalStateException("Git tree read failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git tree read interrupted", exception);
        }
    }
}
