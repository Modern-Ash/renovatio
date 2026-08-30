package org.shark.renovatio.llm.cache;

import java.util.List;

/** Read-only repository history required to verify governed cache promotion. */
public interface PromotionRepository {
    String head();
    boolean isAncestor(String ancestor, String descendant);
    byte[] read(String revision, String repositoryPath);
    List<String> changedPaths(String revision);
}
