package org.shark.renovatio.profile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Canonical JSON, local-first template repository with immutable versions. */
public final class FileProfileTemplateRepository implements ProfileTemplateRepository {
    private final Path root;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public FileProfileTemplateRepository(Path root) {
        if (root == null) throw new IllegalArgumentException("root is required");
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public MigrationProfileTemplate save(MigrationProfileTemplate template) {
        Path file = safeFile(template.reference());
        try {
            Files.createDirectories(root);
            rejectSymlinks(root);
            Files.createDirectories(file.getParent());
            rejectSymlinks(file.getParent());
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(file)) throw new SecurityException("Template path must not be a symlink");
                MigrationProfileTemplate current = read(file);
                if (current.contentHash().equals(template.contentHash())) return current;
                throw new VersionConflictException(template.reference());
            }
            byte[] encoded = json.writerWithDefaultPrettyPrinter().writeValueAsBytes(template);
            Path temporary = Files.createTempFile(file.getParent(), ".template-", ".tmp");
            try {
                Files.write(temporary, encoded, StandardOpenOption.TRUNCATE_EXISTING);
                try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
                moveAtomically(temporary, file);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return template;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save profile template", exception);
        }
    }

    @Override
    public Optional<MigrationProfileTemplate> find(TemplateReference reference) {
        Path file = safeFile(reference);
        try { rejectSymlinks(file.getParent()); }
        catch (IOException exception) { throw new UncheckedIOException("Cannot validate profile template path", exception); }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) return Optional.empty();
        return Optional.of(read(file));
    }

    @Override
    public List<MigrationProfileTemplate> list() {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) return List.of();
        try (var paths = Files.walk(root, 2)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path))
                    .map(this::read)
                    .sorted(Comparator.comparing(MigrationProfileTemplate::name)
                            .thenComparing(MigrationProfileTemplate::version)).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list profile templates", exception);
        }
    }

    private Path safeFile(TemplateReference reference) {
        Path file = root.resolve(reference.name()).resolve(reference.version() + ".json").normalize();
        if (!file.startsWith(root)) throw new SecurityException("Template path escapes configured root");
        return file;
    }

    private MigrationProfileTemplate read(Path file) {
        try {
            MigrationProfileTemplate value = json.readValue(file.toFile(), MigrationProfileTemplate.class);
            MigrationProfileTemplate verified = MigrationProfileTemplate.create(value.name(), value.version(),
                    value.description(), value.profile(), value.createdAt());
            if (!verified.contentHash().equals(value.contentHash())) throw new IllegalStateException("Template hash mismatch");
            return value;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read profile template", exception);
        }
    }

    private static void rejectSymlinks(Path directory) throws IOException {
        Path current = directory;
        while (current != null && Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(current)) throw new SecurityException("Reusable asset root contains a symlink");
            current = current.getParent();
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(source, target); }
    }

    public static final class VersionConflictException extends IllegalArgumentException {
        public VersionConflictException(TemplateReference reference) {
            super("Profile template version already exists with different content: " + reference.name() + "@" + reference.version());
        }
    }
}
