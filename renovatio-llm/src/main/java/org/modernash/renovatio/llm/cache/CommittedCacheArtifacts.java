package org.modernash.renovatio.llm.cache;

/** Immutable runtime view of the technical index and its governed promotion manifest. */
public record CommittedCacheArtifacts(CommittedCacheIndex index,
                                      VerifiedPromotionManifest manifest) { }
