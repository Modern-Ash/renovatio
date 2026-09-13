package org.shark.renovatio.cli.command;

import org.shark.renovatio.application.capability.SurfaceCapabilityRegistry;
import org.shark.renovatio.cli.OutputWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.concurrent.Callable;

@Command(
        name = "capabilities",
        mixinStandardHelpOptions = true,
        description = "List the versioned Renovatio surface capabilities."
)
public final class CapabilitiesCommand implements Callable<Integer> {
    private final SurfaceCapabilityRegistry registry = new SurfaceCapabilityRegistry();

    @Option(names = "--json", description = "Emit the capability contract as JSON.")
    boolean json;

    @Override
    public Integer call() {
        Map<String, Object> document = registry.asMap();
        OutputWriter output = new OutputWriter(json);
        if (json) {
            output.writeJson(document);
            return 0;
        }
        output.line("contract: " + document.get("id") + "@" + document.get("version"));
        @SuppressWarnings("unchecked")
        var capabilities = (java.util.List<Map<String, Object>>) document.get("capabilities");
        for (Map<String, Object> capability : capabilities) {
            output.line(capability.get("id") + " [" + capability.get("maturity") + "]");
        }
        return 0;
    }
}
