package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/** Filesystem cache that fails closed unless a committed envelope matches the verified index. */
public final class ContentAddressedCache {
    private final Path root;
    private final Path quarantine;
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public ContentAddressedCache(Path root, Path quarantine) {
        this.root = root.toAbsolutePath().normalize();
        this.quarantine = quarantine.toAbsolutePath().normalize();
    }

    public Optional<CacheEnvelope> find(String cacheKey, CommittedCacheIndex index,
                                        VerifiedPromotionManifest manifest) {
        CommittedCacheIndex.Entry entry = index.entries().get(cacheKey);
        if (entry == null || !manifest.verifies(cacheKey, index, entry)) {
            return Optional.empty();
        }
        Path expected = pathFor(cacheKey);
        if (!expected.equals(resolveRepositoryPath(entry.repositoryPath())) || !Files.isRegularFile(expected)) {
            return Optional.empty();
        }
        try {
            if (!CacheKey.sha256(Files.readAllBytes(expected)).equals(entry.contentHash())) {
                return Optional.empty();
            }
            CacheEnvelope envelope = json.readValue(expected.toFile(), CacheEnvelope.class);
            if (!cacheKey.equals(envelope.cacheKey())
                    || envelope.promotionDisposition() != PromotionDisposition.COMMITTED
                    || !envelope.hasValidHash()
                    || !envelope.envelopeHash().equals(entry.envelopeHash())) {
                return Optional.empty();
            }
            return Optional.of(envelope);
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    public Path writeCandidate(CacheEnvelope envelope) {
        if (envelope.promotionDisposition() != PromotionDisposition.PENDING_PROMOTION
                || !envelope.hasValidHash()) {
            throw new IllegalArgumentException("Only valid pending envelopes may be written");
        }
        return write(pathFor(envelope.cacheKey()), envelope);
    }

    public Path quarantine(CacheEnvelope envelope) {
        CacheEnvelope invalid = envelope.promotionDisposition() == PromotionDisposition.INVALID_ATTRIBUTION
                ? envelope : envelope.invalidateAttribution();
        Path quarantined = write(quarantine.resolve(invalid.cacheKey() + ".invalid.json"), invalid);
        try {
            Files.deleteIfExists(pathFor(envelope.cacheKey()));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot remove invalid cache candidate", exception);
        }
        return quarantined;
    }

    public Path pathFor(String cacheKey) {
        requireKey(cacheKey);
        return root.resolve(cacheKey.substring(0, 2)).resolve(cacheKey + ".json").normalize();
    }

    private Path resolveRepositoryPath(String repositoryPath) {
        Path resolved = root.resolve(repositoryPath).normalize();
        return resolved.startsWith(root) ? resolved : root.resolve("invalid-path");
    }

    private Path write(Path destination, CacheEnvelope envelope) {
        try {
            Files.createDirectories(destination.getParent());
            Path temporary = Files.createTempFile(destination.getParent(), ".cache-", ".tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), envelope);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return destination;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot persist sanitized cache artifact", exception);
        }
    }

    private static void requireKey(String cacheKey) {
        if (cacheKey == null || !cacheKey.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("cacheKey must be lowercase SHA-256");
        }
    }
}
