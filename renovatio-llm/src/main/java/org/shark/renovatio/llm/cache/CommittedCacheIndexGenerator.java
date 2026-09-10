package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Generates the technical cache index only from an immutable repository tree. */
public final class CommittedCacheIndexGenerator {
    public static final String CACHE_PREFIX = "renovatio-llm/src/main/resources/llm-cache/";
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public CommittedCacheIndex generate(RepositoryTree tree) {
        if (tree.revision() == null || !tree.revision().matches("[0-9a-f]{40,64}")) {
            throw new IllegalStateException("CACHE_INDEX_REVISION_INVALID");
        }
        Map<String, CommittedCacheIndex.Entry> entries = new LinkedHashMap<>();
        for (String path : tree.pathsUnder(CACHE_PREFIX)) {
            if (path.endsWith("committed-cache-index.v1.json")
                    || path.endsWith("verified-cache-promotion.v1.json")) continue;
            if (!path.matches(java.util.regex.Pattern.quote(CACHE_PREFIX)
                    + "[0-9a-f]{2}/[0-9a-f]{64}\\.json")) {
                throw new IllegalStateException("CACHE_INDEX_PATH_INVALID");
            }
            byte[] bytes = tree.read(path);
            try {
                CacheEnvelope envelope = json.readValue(bytes, CacheEnvelope.class);
                String key = envelope.cacheKey();
                String expectedSuffix = key.substring(0, 2) + "/" + key + ".json";
                if (!path.equals(CACHE_PREFIX + expectedSuffix)
                        || envelope.promotionDisposition() != PromotionDisposition.COMMITTED
                        || !envelope.hasValidHash()) {
                    throw new IllegalStateException("CACHE_INDEX_ENVELOPE_INVALID");
                }
                CommittedCacheIndex.Entry previous = entries.put(key,
                        new CommittedCacheIndex.Entry(expectedSuffix, envelope.envelopeHash(),
                                CacheKey.sha256(bytes)));
                if (previous != null) throw new IllegalStateException("CACHE_INDEX_DUPLICATE_KEY");
            } catch (IOException exception) {
                throw new IllegalStateException("CACHE_INDEX_ENVELOPE_INVALID", exception);
            }
        }
        return new CommittedCacheIndex(CommittedCacheIndex.VERSION, entries);
    }
}
