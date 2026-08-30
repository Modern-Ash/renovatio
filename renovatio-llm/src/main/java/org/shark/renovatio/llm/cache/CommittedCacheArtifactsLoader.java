package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Loads cache authority exclusively from the immutable Git HEAD tree and fails closed on drift. */
public final class CommittedCacheArtifactsLoader {
    public static final String INDEX_PATH = CommittedCacheIndexGenerator.CACHE_PREFIX
            + "committed-cache-index.v1.json";
    public static final String MANIFEST_PATH = CommittedCacheIndexGenerator.CACHE_PREFIX
            + "verified-cache-promotion.v1.json";
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public CommittedCacheArtifacts load(RepositoryTree tree) {
        List<String> paths = tree.pathsUnder(CommittedCacheIndexGenerator.CACHE_PREFIX);
        boolean hasIndex = paths.contains(INDEX_PATH);
        boolean hasManifest = paths.contains(MANIFEST_PATH);
        if (!hasIndex && !hasManifest) return empty();
        if (hasIndex != hasManifest) throw new IllegalStateException("CACHE_AUTHORITY_INCOMPLETE");
        try {
            CommittedCacheIndex storedIndex = json.readValue(tree.read(INDEX_PATH),
                    CommittedCacheIndex.class);
            VerifiedPromotionManifest manifest = json.readValue(tree.read(MANIFEST_PATH),
                    VerifiedPromotionManifest.class);
            CommittedCacheIndex generated = new CommittedCacheIndexGenerator().generate(tree);
            if (!storedIndex.equals(generated)
                    || !manifest.indexHash().equals(storedIndex.digest())
                    || !manifest.entries().keySet().equals(storedIndex.entries().keySet())) {
                throw new IllegalStateException("CACHE_AUTHORITY_DRIFT");
            }
            return new CommittedCacheArtifacts(storedIndex, manifest);
        } catch (IOException exception) {
            throw new IllegalStateException("CACHE_AUTHORITY_INVALID", exception);
        }
    }

    private static CommittedCacheArtifacts empty() {
        CommittedCacheIndex index = new CommittedCacheIndex(CommittedCacheIndex.VERSION, Map.of());
        return new CommittedCacheArtifacts(index, new VerifiedPromotionManifest(
                VerifiedPromotionManifest.VERSION, index.digest(), Map.of()));
    }
}
