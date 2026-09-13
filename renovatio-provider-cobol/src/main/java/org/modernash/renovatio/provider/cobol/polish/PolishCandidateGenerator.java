package org.modernash.renovatio.provider.cobol.polish;

@FunctionalInterface
public interface PolishCandidateGenerator {
    PolishCandidate generate(PolishProposalRequest request);
}
