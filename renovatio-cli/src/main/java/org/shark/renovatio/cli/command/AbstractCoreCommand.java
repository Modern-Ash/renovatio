package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.OutputWriter;
import org.shark.renovatio.cli.RenovatioCliContext;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/** Common wiring for subcommands that route a single tool call to the in-process core. */
public abstract class AbstractCoreCommand implements Callable<Integer> {

    @Option(names = "--json", description = "Emit the raw result as JSON instead of human-readable text.")
    protected boolean json;

    /** Overridable for tests so the context / registry can be faked. */
    protected RenovatioCliContext context() {
        return RenovatioCliContext.shared();
    }

    protected OutputWriter output() {
        return new OutputWriter(json);
    }

    protected Map<String, Object> route(String tool, Map<String, Object> args) {
        return context().registry().routeToolCall(tool, args);
    }

    protected static Map<String, Object> args() {
        return new LinkedHashMap<>();
    }

    protected static String absolute(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    protected static void putIfPresent(Map<String, Object> args, String key, Object value) {
        if (value != null && !value.toString().isBlank()) {
            args.put(key, value);
        }
    }
}
