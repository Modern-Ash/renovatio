package org.shark.renovatio.provider.cobol.polish;

import org.shark.renovatio.provider.cobol.guardrail.GateCheckResult;

@FunctionalInterface
public interface PolishCandidateCheck {
    GateCheckResult validate(PolishProposalRequest request, PolishCandidate candidate);
}
