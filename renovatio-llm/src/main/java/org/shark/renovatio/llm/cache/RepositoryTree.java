package org.shark.renovatio.llm.cache;

import java.util.List;

/** Read-only view of one immutable repository revision. */
public interface RepositoryTree {
    String revision();
    List<String> pathsUnder(String repositoryPrefix);
    byte[] read(String repositoryPath);
}
