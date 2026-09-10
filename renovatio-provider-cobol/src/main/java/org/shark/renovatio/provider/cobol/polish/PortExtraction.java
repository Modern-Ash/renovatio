package org.shark.renovatio.provider.cobol.polish;

import javax.lang.model.SourceVersion;
import java.util.Set;

public record PortExtraction(
        String dependencyBoundary,
        String interfaceName,
        Set<String> callSites,
        boolean contractPreserved,
        boolean addsDependencyOrConfiguration) implements PolishFamilyPayload {

    public PortExtraction {
        dependencyBoundary = PolishContracts.nonBlank(dependencyBoundary, "dependencyBoundary");
        interfaceName = PolishContracts.nonBlank(interfaceName, "interfaceName");
        callSites = PolishContracts.javaPaths(callSites, "callSites");
        if (!SourceVersion.isIdentifier(interfaceName) || SourceVersion.isKeyword(interfaceName)) {
            throw new IllegalArgumentException("interfaceName must be a legal Java identifier");
        }
        if (!contractPreserved) {
            throw new IllegalArgumentException("Port extraction must preserve the existing contract");
        }
        if (addsDependencyOrConfiguration) {
            throw new IllegalArgumentException("Port extraction cannot add a dependency or configuration");
        }
    }

    @Override
    public PolishProposalFamily family() {
        return PolishProposalFamily.PORT_EXTRACTION;
    }
}
