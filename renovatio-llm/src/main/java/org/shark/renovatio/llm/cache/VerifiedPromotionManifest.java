package org.shark.renovatio.llm.cache;

import java.util.Map;

/** Build-produced proof that technical index entries passed governed A/B/C promotion checks. */
public record VerifiedPromotionManifest(String manifestVersion, String indexHash,
                                        Map<String, Entry> entries) {
    public static final String VERSION = "verified-cache-promotion.v1";

    public VerifiedPromotionManifest {
        if (!VERSION.equals(manifestVersion)) throw new IllegalArgumentException("Unsupported manifest");
        if (indexHash == null || !indexHash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("indexHash");
        entries = Map.copyOf(entries);
    }

    public boolean verifies(String key, CommittedCacheIndex index, CommittedCacheIndex.Entry indexed) {
        Entry proof = entries.get(key);
        return index.digest().equals(indexHash) && proof != null
                && proof.envelopeHash().equals(indexed.envelopeHash())
                && proof.contentHash().equals(indexed.contentHash())
                && proof.repositoryPath().equals(indexed.repositoryPath())
                && proof.commitA().matches("[0-9a-f]{40,64}")
                && proof.commitB().matches("[0-9a-f]{40,64}")
                && proof.commitC().matches("[0-9a-f]{40,64}")
                && proof.approvalEvidenceRef().startsWith("repo://.agora/");
    }

    public record Entry(String repositoryPath, String envelopeHash, String contentHash,
                        String commitA, String commitB, String commitC,
                        String approvalEvidenceRef) { }
}
