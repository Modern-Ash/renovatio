package org.shark.renovatio.decisions;

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

/** Local canonical JSON repository for immutable decision-policy catalogs. */
public final class FileDecisionPolicyRepository implements DecisionPolicyRepository {
    private final Path root;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public FileDecisionPolicyRepository(Path root) {
        if (root == null) throw new IllegalArgumentException("root is required");
        this.root = root.toAbsolutePath().normalize();
    }

    @Override public DecisionPolicyCatalog save(DecisionPolicyCatalog catalog) {
        Path file = safeFile(catalog.reference());
        try {
            Files.createDirectories(root);
            rejectSymlinks(root);
            Files.createDirectories(file.getParent());
            rejectSymlinks(file.getParent());
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(file)) throw new SecurityException("Policy path must not be a symlink");
                DecisionPolicyCatalog current = read(file);
                if (current.contentHash().equals(catalog.contentHash())) return current;
                throw new VersionConflictException(catalog.reference());
            }
            byte[] encoded = json.writerWithDefaultPrettyPrinter().writeValueAsBytes(catalog);
            Path temporary = Files.createTempFile(file.getParent(), ".policy-", ".tmp");
            try {
                Files.write(temporary, encoded, StandardOpenOption.TRUNCATE_EXISTING);
                try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
                moveAtomically(temporary, file);
            } finally { Files.deleteIfExists(temporary); }
            return catalog;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save decision-policy catalog", exception);
        }
    }

    @Override public Optional<DecisionPolicyCatalog> find(PolicyReference reference) {
        Path file = safeFile(reference);
        try { rejectSymlinks(file.getParent()); }
        catch (IOException exception) { throw new UncheckedIOException("Cannot validate decision-policy path", exception); }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) return Optional.empty();
        return Optional.of(read(file));
    }

    @Override public List<DecisionPolicyCatalog> list() {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) return List.of();
        try (var paths = Files.walk(root, 2)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path))
                    .map(this::read).sorted(Comparator.comparing(DecisionPolicyCatalog::name)
                            .thenComparing(DecisionPolicyCatalog::version)).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list decision-policy catalogs", exception);
        }
    }

    private Path safeFile(PolicyReference reference) {
        Path file = root.resolve(reference.name()).resolve(reference.version() + ".json").normalize();
        if (!file.startsWith(root)) throw new SecurityException("Policy path escapes configured root");
        return file;
    }

    private DecisionPolicyCatalog read(Path file) {
        try {
            DecisionPolicyCatalog value = json.readValue(file.toFile(), DecisionPolicyCatalog.class);
            DecisionPolicyCatalog verified = DecisionPolicyCatalog.create(value.name(), value.version(),
                    value.analyzerVersion(), value.autoConfirmThreshold(), value.suggestThreshold(),
                    value.entries(), value.createdAt());
            if (!verified.contentHash().equals(value.contentHash())) throw new IllegalStateException("Policy hash mismatch");
            return value;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read decision-policy catalog", exception);
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
        public VersionConflictException(PolicyReference reference) {
            super("Decision-policy version already exists with different content: " + reference.name() + "@" + reference.version());
        }
    }
}
