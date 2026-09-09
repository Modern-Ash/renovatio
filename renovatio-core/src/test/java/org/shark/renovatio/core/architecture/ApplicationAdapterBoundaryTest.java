package org.shark.renovatio.core.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationAdapterBoundaryTest {
    @Test void productiveTransportsDependOnApplicationBusNotProviderRouting() throws Exception {
        List<Path> adapters = List.of(
                Path.of("../renovatio-cli/src/main/java/org/shark/renovatio/cli/command/AbstractCoreCommand.java"),
                Path.of("../renovatio-mcp-server/src/main/java/org/shark/renovatio/mcp/server/service/McpToolingService.java"),
                Path.of("../renovatio-api/src/main/java/org/shark/renovatio/api/service/PersistentPlanService.java"));
        for (Path adapter : adapters) {
            String source = Files.readString(adapter);
            assertTrue(source.contains("ApplicationCommandBus") || source.contains(".application().execute("),
                    adapter + " must use application boundary");
            assertFalse(source.contains(".routeToolCall("), adapter + " must not route providers directly");
            assertFalse(source.contains("MigrationPlanService "), adapter + " must not orchestrate legacy service directly");
        }
    }
}
