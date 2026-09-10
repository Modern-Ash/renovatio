package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommittedCacheArtifactsLoaderTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void loadsOnlyIndexAndManifestThatMatchGeneratedHeadTree() throws Exception {
        CacheEnvelope envelope = envelope().promote();
        String path = CommittedCacheIndexGenerator.CACHE_PREFIX + envelope.cacheKey().substring(0, 2)
                + "/" + envelope.cacheKey() + ".json";
        byte[] envelopeBytes = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(envelope);
        MemoryTree seed = new MemoryTree(Map.of(path, envelopeBytes));
        CommittedCacheIndex index = new CommittedCacheIndexGenerator().generate(seed);
        CommittedCacheIndex.Entry entry = index.entries().get(envelope.cacheKey());
        VerifiedPromotionManifest manifest = new VerifiedPromotionManifest(
                VerifiedPromotionManifest.VERSION, index.digest(), Map.of(envelope.cacheKey(),
                new VerifiedPromotionManifest.Entry(entry.repositoryPath(), entry.envelopeHash(),
                        entry.contentHash(), "a".repeat(40), "b".repeat(40), "c".repeat(40),
                        "repo://.agora/evidence/cache-promotion.md")));
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put(path, envelopeBytes);
        files.put(CommittedCacheArtifactsLoader.INDEX_PATH, JSON.writeValueAsBytes(index));
        files.put(CommittedCacheArtifactsLoader.MANIFEST_PATH, JSON.writeValueAsBytes(manifest));

        CommittedCacheArtifacts loaded = new CommittedCacheArtifactsLoader().load(new MemoryTree(files));

        assertEquals(index, loaded.index());
        assertEquals(manifest, loaded.manifest());
    }

    @Test
    void failsClosedForIncompleteOrDriftingAuthority() throws Exception {
        CommittedCacheIndex empty = new CommittedCacheIndex(CommittedCacheIndex.VERSION, Map.of());
        assertThrows(IllegalStateException.class, () -> new CommittedCacheArtifactsLoader().load(
                new MemoryTree(Map.of(CommittedCacheArtifactsLoader.INDEX_PATH,
                        JSON.writeValueAsBytes(empty)))));

        VerifiedPromotionManifest wrong = new VerifiedPromotionManifest(
                VerifiedPromotionManifest.VERSION, "f".repeat(64), Map.of());
        assertThrows(IllegalStateException.class, () -> new CommittedCacheArtifactsLoader().load(
                new MemoryTree(Map.of(
                        CommittedCacheArtifactsLoader.INDEX_PATH, JSON.writeValueAsBytes(empty),
                        CommittedCacheArtifactsLoader.MANIFEST_PATH, JSON.writeValueAsBytes(wrong)))));
    }

    @Test
    void repositoryWithoutAuthorityHasAnEmptyFailClosedView() {
        CommittedCacheArtifacts loaded = new CommittedCacheArtifactsLoader().load(new MemoryTree(Map.of()));
        assertEquals(Map.of(), loaded.index().entries());
        assertEquals(Map.of(), loaded.manifest().entries());
    }

    private static CacheEnvelope envelope() {
        String key = CacheKey.sha256("key");
        CacheIdentity identity = new CacheIdentity(JSON.createObjectNode().put("nodeId", "node-1"),
                "prompt.v1", "schema.v1", "a".repeat(64), List.of("json-schema.v1"),
                "offline-fake", "fixture-v1");
        return CacheEnvelope.pending(key, ResultDisposition.MODEL_SUCCESS, identity,
                CacheKey.sha256("input"), CacheKey.sha256("output"), null,
                JSON.createObjectNode().put("rationale", "sanitized"), "tool-test",
                "repo://renovatio-llm/src/main/resources/llm-cache/" + key.substring(0, 2)
                        + "/" + key + ".json");
    }

    private record MemoryTree(Map<String, byte[]> files) implements RepositoryTree {
        private MemoryTree { files = Map.copyOf(files); }
        @Override public String revision() { return "d".repeat(40); }
        @Override public List<String> pathsUnder(String prefix) {
            return files.keySet().stream().filter(path -> path.startsWith(prefix)).sorted().toList();
        }
        @Override public byte[] read(String repositoryPath) { return files.get(repositoryPath); }
    }
}
