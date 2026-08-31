package org.shark.renovatio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Centralises the human-vs-JSON rendering decision and the exit-code mapping shared by every
 * subcommand.
 *
 * <ul>
 *   <li>exit {@code 0} when the result map reports {@code success == true};</li>
 *   <li>exit {@code 1} when the result is {@code null}/empty or {@code success != true}.</li>
 * </ul>
 */
public final class OutputWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private final PrintStream out;
    private final PrintStream err;
    private final boolean json;

    public OutputWriter(boolean json) {
        this(json, System.out, System.err);
    }

    public OutputWriter(boolean json, PrintStream out, PrintStream err) {
        this.json = json;
        this.out = out;
        this.err = err;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** Renders a routed tool-call result and returns the process exit code. */
    public int render(Map<String, Object> result, Consumer<Map<String, Object>> humanRenderer) {
        boolean ok = result != null && !result.isEmpty() && Boolean.TRUE.equals(result.get("success"));
        if (json) {
            writeJson(result == null ? Map.of() : result);
        } else if (ok) {
            humanRenderer.accept(result);
        } else {
            err.println("error: " + failureMessage(result));
        }
        return ok ? 0 : 1;
    }

    public void writeJson(Object value) {
        try {
            out.println(MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            err.println("error: could not serialise result: " + e.getMessage());
        }
    }

    public void line(String text) {
        out.println(text);
    }

    private static String failureMessage(Map<String, Object> result) {
        if (result == null || result.isEmpty()) {
            return "no result returned by the engine";
        }
        for (String key : new String[] {"message", "error", "summary"}) {
            Object v = result.get(key);
            if (v != null && !v.toString().isBlank()) {
                return v.toString();
            }
        }
        return "operation failed";
    }
}
