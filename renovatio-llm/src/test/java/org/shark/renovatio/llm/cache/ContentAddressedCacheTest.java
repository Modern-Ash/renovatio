package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentAddressedCacheTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @TempDir
    Path temporary;

    @Test
    void everyIdentityComponentInvalidatesTheKey() {
        CacheIdentity base = identity("prompt.v1", "schema.v1", HASH_A,
                List.of("json-schema.v1"), "anthropic", "model-a", "value");
        String baseKey = CacheKey.derive(base);
        List<CacheIdentity> changes = List.of(
                identity("prompt.v2", "schema.v1", HASH_A, List.of("json-schema.v1"), "anthropic", "model-a", "value"),
                identity("prompt.v1", "schema.v2", HASH_A, List.of("json-schema.v1"), "anthropic", "model-a", "value"),
                identity("prompt.v1", "schema.v1", HASH_B, List.of("json-schema.v1"), "anthropic", "model-a", "value"),
                identity("prompt.v1", "schema.v1", HASH_A, List.of("sanitized-persistence.v1"), "anthropic", "model-a", "value"),
                identity("prompt.v1", "schema.v1", HASH_A, List.of("json-schema.v1"), "fake", "model-a", "value"),
                identity("prompt.v1", "schema.v1", HASH_A, List.of("json-schema.v1"), "anthropic", "model-b", "value"),
                identity("prompt.v1", "schema.v1", HASH_A, List.of("json-schema.v1"), "anthropic", "model-a", "other"));

        changes.forEach(change -> assertNotEquals(baseKey, CacheKey.derive(change)));
        assertEquals(baseKey, CacheKey.derive(base));
    }

    @Test
    void onlyIndexedCommittedEnvelopeCanProduceAHit() throws Exception {
        ContentAddressedCache cache = cache();
        CacheEnvelope pending = pending();
        Path path = cache.writeCandidate(pending);
        String relative = cacheRoot().relativize(path).toString();
        CommittedCacheIndex pendingIndex = index(pending.cacheKey(), relative, pending.envelopeHash(),
                CacheKey.sha256(Files.readAllBytes(path)));

        assertTrue(cache.find(pending.cacheKey(), pendingIndex, manifest(pendingIndex, pending.cacheKey())).isEmpty());

        CacheEnvelope committed = pending.promote();
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), committed);
        CommittedCacheIndex committedIndex = index(committed.cacheKey(), relative, committed.envelopeHash(),
                CacheKey.sha256(Files.readAllBytes(path)));

        assertEquals(committed, cache.find(committed.cacheKey(), committedIndex,
                manifest(committedIndex, committed.cacheKey())).orElseThrow());
        CommittedCacheIndex wrong = index(committed.cacheKey(), relative, HASH_A,
                CacheKey.sha256(Files.readAllBytes(path)));
        assertTrue(cache.find(committed.cacheKey(), wrong, manifest(wrong, committed.cacheKey())).isEmpty());
        CommittedCacheIndex empty = new CommittedCacheIndex(CommittedCacheIndex.VERSION, Map.of());
        assertTrue(cache.find(committed.cacheKey(), empty, emptyManifest(empty)).isEmpty());
    }

    @Test
    void attributionFailureMovesAnInvalidEnvelopeOutsideLookupTree() throws Exception {
        ContentAddressedCache cache = cache();
        CacheEnvelope pending = pending();
        Path candidate = cache.writeCandidate(pending);
        Path quarantined = cache.quarantine(pending);
        CacheEnvelope invalid = JSON.readValue(quarantined.toFile(), CacheEnvelope.class);

        assertFalse(Files.exists(candidate));
        assertTrue(quarantined.startsWith(temporary.resolve("quarantine")));
        assertEquals(PromotionDisposition.INVALID_ATTRIBUTION, invalid.promotionDisposition());
        assertTrue(invalid.hasValidHash());
        CommittedCacheIndex invalidIndex = index(invalid.cacheKey(),
                cacheRoot().relativize(candidate).toString(), invalid.envelopeHash(), HASH_A);
        assertFalse(cache.find(invalid.cacheKey(), invalidIndex,
                manifest(invalidIndex, invalid.cacheKey())).isPresent());
    }

    @Test
    void canonicalKeyIsIndependentOfObjectMemberOrder() {
        LinkedHashMap<String, Object> first = new LinkedHashMap<>();
        first.put("b", 2);
        first.put("a", 1);
        LinkedHashMap<String, Object> second = new LinkedHashMap<>();
        second.put("a", 1);
        second.put("b", 2);
        CacheIdentity left = new CacheIdentity(JSON.valueToTree(first), "prompt.v1", "schema.v1",
                HASH_A, List.of("json-schema.v1"), "anthropic", "model");
        CacheIdentity right = new CacheIdentity(JSON.valueToTree(second), "prompt.v1", "schema.v1",
                HASH_A, List.of("json-schema.v1"), "anthropic", "model");

        assertEquals(CacheKey.derive(left), CacheKey.derive(right));
    }

    @Test
    void indexGeneratorUsesOnlyImmutableTreePathsAndTrackedBytes() throws Exception {
        CacheEnvelope committed = pending().promote();
        byte[] tracked = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(committed);
        String suffix = committed.cacheKey().substring(0, 2) + "/" + committed.cacheKey() + ".json";
        String path = CommittedCacheIndexGenerator.CACHE_PREFIX + suffix;
        AtomicInteger reads = new AtomicInteger();
        RepositoryTree tree = new RepositoryTree() {
            @Override public String revision() { return "d".repeat(40); }
            @Override public List<String> pathsUnder(String prefix) { return List.of(path); }
            @Override public byte[] read(String requested) { reads.incrementAndGet(); return tracked; }
        };

        CommittedCacheIndex generated = new CommittedCacheIndexGenerator().generate(tree);

        assertEquals(1, reads.get());
        assertEquals(suffix, generated.entries().get(committed.cacheKey()).repositoryPath());
        assertEquals(CacheKey.sha256(tracked), generated.entries().get(committed.cacheKey()).contentHash());
    }

    @Test
    void indexGeneratorRejectsTreePathThatDoesNotMatchEnvelopeIdentity() throws Exception {
        CacheEnvelope committed = pending().promote();
        byte[] tracked = JSON.writeValueAsBytes(committed);
        String wrongKey = "e".repeat(64);
        RepositoryTree tree = new RepositoryTree() {
            @Override public String revision() { return "d".repeat(40); }
            @Override public List<String> pathsUnder(String prefix) {
                return List.of(CommittedCacheIndexGenerator.CACHE_PREFIX + "ee/" + wrongKey + ".json");
            }
            @Override public byte[] read(String requested) { return tracked; }
        };

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new CommittedCacheIndexGenerator().generate(tree));
    }

    private ContentAddressedCache cache() {
        return new ContentAddressedCache(cacheRoot(), temporary.resolve("quarantine"));
    }

    private Path cacheRoot() {
        return temporary.resolve("cache");
    }

    private static CacheEnvelope pending() {
        String inputHash = CacheKey.sha256("input");
        String outputHash = CacheKey.sha256("output");
        String key = CacheKey.sha256("key");
        CacheIdentity identity = identity("prompt.v1", "schema.v1", HASH_A,
                List.of("json-schema.v1"), "anthropic", "model", "input");
        return CacheEnvelope.pending(key, ResultDisposition.DETERMINISTIC_FALLBACK,
                identity, inputHash, outputHash, "PROVIDER_TIMEOUT",
                JSON.createObjectNode().put("manualAction", "Review manually"), "tool-test",
                "repo://renovatio-llm/src/main/resources/llm-cache/" + key.substring(0, 2)
                        + "/" + key + ".json");
    }

    private static CacheIdentity identity(String prompt, String schema, String schemaHash,
                                          List<String> validators, String provider, String model,
                                          String input) {
        return new CacheIdentity(JSON.createObjectNode().put("input", input), prompt, schema,
                schemaHash, validators, provider, model);
    }

    private static CommittedCacheIndex index(String key, String path, String envelopeHash,
                                             String contentHash) {
        return new CommittedCacheIndex(CommittedCacheIndex.VERSION,
                Map.of(key, new CommittedCacheIndex.Entry(path, envelopeHash, contentHash)));
    }

    private static VerifiedPromotionManifest manifest(CommittedCacheIndex index, String key) {
        CommittedCacheIndex.Entry entry = index.entries().get(key);
        return new VerifiedPromotionManifest(VerifiedPromotionManifest.VERSION, index.digest(), Map.of(key,
                new VerifiedPromotionManifest.Entry(entry.repositoryPath(), entry.envelopeHash(),
                        entry.contentHash(), "a".repeat(40), "b".repeat(40), "c".repeat(40),
                        "repo://.agora/evidence/cache-promotion.md")));
    }

    private static VerifiedPromotionManifest emptyManifest(CommittedCacheIndex index) {
        return new VerifiedPromotionManifest(VerifiedPromotionManifest.VERSION, index.digest(), Map.of());
    }
}
