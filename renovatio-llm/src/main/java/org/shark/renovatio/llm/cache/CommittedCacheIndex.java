package org.shark.renovatio.llm.cache;

import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;
import java.util.LinkedHashMap;
import java.util.Map;

/** Build-verified technical index; approval is bound by the separate promotion manifest. */
public record CommittedCacheIndex(String indexVersion, Map<String, Entry> entries) {
    public static final String VERSION = "committed-cache-index.v1";

    public CommittedCacheIndex {
        entries = Map.copyOf(entries);
        if (!VERSION.equals(indexVersion)) throw new IllegalArgumentException("Unsupported cache index");
    }

    public String digest() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("indexVersion", indexVersion);
        Map<String, Object> projectedEntries = new LinkedHashMap<>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(item -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("repositoryPath", item.getValue().repositoryPath());
            entry.put("envelopeHash", item.getValue().envelopeHash());
            entry.put("contentHash", item.getValue().contentHash());
            projectedEntries.put(item.getKey(), entry);
        });
        projection.put("entries", projectedEntries);
        return CacheKey.sha256(CanonicalJson.write(projection));
    }

    public record Entry(String repositoryPath, String envelopeHash, String contentHash) {
        public Entry {
            if (repositoryPath == null || repositoryPath.isBlank()) throw new IllegalArgumentException("path");
            requireHash(envelopeHash);
            requireHash(contentHash);
        }

        private static void requireHash(String value) {
            if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("hash");
        }
    }
}
