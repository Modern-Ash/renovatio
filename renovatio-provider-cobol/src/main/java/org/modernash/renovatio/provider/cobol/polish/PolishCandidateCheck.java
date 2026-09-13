package org.modernash.renovatio.provider.cobol.polish;

import org.modernash.renovatio.provider.cobol.guardrail.GateCheckResult;

@FunctionalInterface
public interface PolishCandidateCheck {
    GateCheckResult validate(PolishProposalRequest request, PolishCandidate candidate);
}
