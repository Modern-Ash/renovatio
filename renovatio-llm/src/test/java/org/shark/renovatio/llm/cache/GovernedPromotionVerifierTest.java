package org.shark.renovatio.llm.cache;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GovernedPromotionVerifierTest {
    @Test
    void buildVerifiesCommittedPromotionAgainstGitAndAgoraHistory() {
        Path repository = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".."))
                .toAbsolutePath().normalize();
        CommittedCacheArtifacts authority = new CommittedCacheArtifactsLoader()
                .load(new GitHeadRepositoryTree(repository));

        new GovernedPromotionVerifier().verify(new GitPromotionRepository(repository), authority);

        assertEquals(1, authority.index().entries().size());
        assertEquals(authority.index().entries().keySet(), authority.manifest().entries().keySet());
    }
}
