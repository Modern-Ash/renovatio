package org.shark.renovatio.provider.cobol.polish;

import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;

import java.nio.file.Path;
import java.util.List;

public record PolishProposalOutcome(
        PolishDisposition disposition,
        String proposalId,
        Path artifactDirectory,
        GuardrailGate failedGate,
        String diagnosticReference,
        List<GuardrailGate> executedGates,
        String originalTreeHash,
        String retainedTreeHash,
        List<ManualActionItem> actionItems) {

    public PolishProposalOutcome {
        executedGates = List.copyOf(executedGates);
        originalTreeHash = PolishContracts.hash(originalTreeHash, "originalTreeHash");
        retainedTreeHash = PolishContracts.hash(retainedTreeHash, "retainedTreeHash");
        actionItems = actionItems.stream().sorted().toList();
    }
}
