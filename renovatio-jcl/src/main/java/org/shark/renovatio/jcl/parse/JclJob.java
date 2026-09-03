package org.shark.renovatio.jcl.parse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parsed JCL job AST. */
public record JclJob(String jobName, String sourcePath, String sourceSha256, List<JclStep> steps,
                     List<UnresolvedProc> unresolvedProcs, Map<String, String> symbols) {
    public JclJob {
        if (jobName == null || jobName.isBlank()) throw new IllegalArgumentException("jobName is required");
        jobName = jobName.toUpperCase(Locale.ROOT);
        if (sourcePath == null || sourcePath.isBlank()) throw new IllegalArgumentException("sourcePath is required");
        if (sourceSha256 == null || !sourceSha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("sourceSha256 must be lowercase SHA-256");
        steps = steps == null ? List.of() : List.copyOf(steps);
        unresolvedProcs = unresolvedProcs == null ? List.of() : List.copyOf(unresolvedProcs);
        symbols = symbols == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(symbols));
    }

    public JclJob(String jobName, String sourcePath, List<JclStep> steps,
                  List<UnresolvedProc> unresolvedProcs, Map<String, String> symbols) {
        this(jobName, sourcePath, JclParser.sha256(sourcePath), steps, unresolvedProcs, symbols);
    }

    public record UnresolvedProc(String stepName, String procName, int sourceLine) {
        public UnresolvedProc {
            stepName = stepName.toUpperCase(Locale.ROOT);
            procName = procName.toUpperCase(Locale.ROOT);
        }
    }
}
