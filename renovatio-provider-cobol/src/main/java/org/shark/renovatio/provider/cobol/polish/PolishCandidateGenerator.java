package org.shark.renovatio.provider.cobol.polish;

@FunctionalInterface
public interface PolishCandidateGenerator {
    PolishCandidate generate(PolishProposalRequest request);
}
