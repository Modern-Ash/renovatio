package org.shark.renovatio.provider.cobol.polish;

import javax.lang.model.SourceVersion;
import java.util.Set;

public record DomainNamingRefinement(
        String nodeId,
        String currentSymbol,
        String proposedSymbol,
        Set<String> referencePaths,
        boolean collisionFree,
        boolean publicSignature,
        boolean ownerApproved) implements PolishFamilyPayload {

    public DomainNamingRefinement {
        nodeId = PolishContracts.nonBlank(nodeId, "nodeId");
        currentSymbol = PolishContracts.nonBlank(currentSymbol, "currentSymbol");
        proposedSymbol = PolishContracts.nonBlank(proposedSymbol, "proposedSymbol");
        referencePaths = PolishContracts.javaPaths(referencePaths, "referencePaths");
        if (!SourceVersion.isIdentifier(proposedSymbol) || SourceVersion.isKeyword(proposedSymbol)) {
            throw new IllegalArgumentException("proposedSymbol must be a legal Java identifier");
        }
        if (!collisionFree) {
            throw new IllegalArgumentException("proposedSymbol must be collision free");
        }
        if (publicSignature && !ownerApproved) {
            throw new IllegalArgumentException("Public signature rename requires owner approval");
        }
    }

    @Override
    public PolishProposalFamily family() {
        return PolishProposalFamily.DOMAIN_NAMING_REFINEMENT;
    }
}
