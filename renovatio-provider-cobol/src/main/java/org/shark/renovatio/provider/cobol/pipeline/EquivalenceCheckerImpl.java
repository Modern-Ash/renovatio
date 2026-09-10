package org.shark.renovatio.provider.cobol.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Implementation of EquivalenceChecker for comparing generated and expected output.
 */
public class EquivalenceCheckerImpl implements EquivalenceChecker {
    
    private static final Logger log = LoggerFactory.getLogger(EquivalenceCheckerImpl.class);
    private final MetadataNormalizer normalizer = new MetadataNormalizer();
    
    @Override
    public EquivalenceReport check(Path actualPath, Path expectedPath, EquivalenceConfig config) {
        log.debug("Checking equivalence: {} vs {}", actualPath, expectedPath);
        
        try {
            String actualContent = Files.readString(actualPath);
            String expectedContent = Files.readString(expectedPath);
            
            // Normalize content
            String normalizedActual = normalizer.normalizeFile(actualPath, actualContent);
            String normalizedExpected = normalizer.normalizeFile(expectedPath, expectedContent);
            
            // Compare line by line
            List<EquivalenceReport.LineDivergence> divergences = compareLines(
                normalizedActual, normalizedExpected, config);
            
            boolean byteIdentical = actualContent.equals(expectedContent);
            boolean contentIdentical = normalizedActual.equals(normalizedExpected);
            
            // Determine gate decision
            EquivalenceReport.GateDecision decision;
            if (contentIdentical) {
                decision = EquivalenceReport.GateDecision.PASS;
            } else if (divergences.stream().anyMatch(d -> 
                d.type() == EquivalenceReport.LineDivergence.DivergenceType.CONTENT)) {
                decision = EquivalenceReport.GateDecision.FAIL;
            } else {
                decision = EquivalenceReport.GateDecision.PASS;
            }
            
            return new EquivalenceReport(
                actualPath.getFileName().toString(),
                actualPath,
                expectedPath,
                byteIdentical,
                divergences,
                config.excludedMetadataPatterns(),
                decision
            );
            
        } catch (IOException e) {
            log.error("Error reading files for equivalence check", e);
            return new EquivalenceReport(
                actualPath.getFileName().toString(),
                actualPath,
                expectedPath,
                false,
                List.of(),
                List.of(),
                EquivalenceReport.GateDecision.FAIL
            );
        }
    }
    
    @Override
    public List<EquivalenceReport> checkAll(String fixtureId, Path actualDir, Path expectedDir, 
                                           EquivalenceConfig config) {
        List<EquivalenceReport> reports = new ArrayList<>();
        
        try {
            Map<Path, Path> actualFiles = indexJavaFiles(actualDir);
            Map<Path, Path> expectedFiles = indexJavaFiles(expectedDir);
            TreeSet<Path> allPaths = new TreeSet<>();
            allPaths.addAll(actualFiles.keySet());
            allPaths.addAll(expectedFiles.keySet());

            for (Path relativePath : allPaths) {
                Path actualFile = actualFiles.get(relativePath);
                Path expectedFile = expectedFiles.get(relativePath);

                if (actualFile == null) {
                    reports.add(fileSetMismatch(
                        fixtureId, actualDir.resolve(relativePath), expectedFile,
                        "Generated output is missing expected file", relativePath
                    ));
                } else if (expectedFile == null) {
                    reports.add(fileSetMismatch(
                        fixtureId, actualFile, expectedDir.resolve(relativePath),
                        "Generated output contains unexpected file", relativePath
                    ));
                } else {
                    reports.add(check(actualFile, expectedFile, config));
                }
            }
            
        } catch (IOException e) {
            log.error("Error finding Java files for equivalence check", e);
            reports.add(fileSetMismatch(
                fixtureId, actualDir, expectedDir,
                "Unable to enumerate files for equivalence: " + e.getMessage(), Path.of(".")
            ));
        }
        
        return reports;
    }

    private Map<Path, Path> indexJavaFiles(Path directory) throws IOException {
        Map<Path, Path> javaFiles = new TreeMap<>();
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.filter(Files::isRegularFile)
                      .filter(path -> path.toString().endsWith(".java"))
                      .forEach(path -> javaFiles.put(directory.relativize(path), path));
            }
        }
        return javaFiles;
    }

    private EquivalenceReport fileSetMismatch(String fixtureId, Path actualPath, Path expectedPath,
                                              String message, Path relativePath) {
        return new EquivalenceReport(
            fixtureId,
            actualPath,
            expectedPath,
            false,
            List.of(new EquivalenceReport.LineDivergence(
                0,
                message,
                relativePath.toString(),
                EquivalenceReport.LineDivergence.DivergenceType.CONTENT
            )),
            List.of(),
            EquivalenceReport.GateDecision.FAIL
        );
    }
    
    private List<EquivalenceReport.LineDivergence> compareLines(String actual, String expected, 
                                                               EquivalenceConfig config) {
        List<EquivalenceReport.LineDivergence> divergences = new ArrayList<>();
        
        String[] actualLines = actual.split("\n");
        String[] expectedLines = expected.split("\n");
        
        int maxLines = Math.max(actualLines.length, expectedLines.length);
        
        for (int i = 0; i < maxLines; i++) {
            String actualLine = i < actualLines.length ? actualLines[i] : "";
            String expectedLine = i < expectedLines.length ? expectedLines[i] : "";
            
            if (!actualLine.equals(expectedLine)) {
                EquivalenceReport.LineDivergence.DivergenceType type = 
                    determineDivergenceType(actualLine, expectedLine, config);
                
                divergences.add(new EquivalenceReport.LineDivergence(
                    i + 1,
                    actualLine,
                    expectedLine,
                    type
                ));
            }
        }
        
        return divergences;
    }
    
    private EquivalenceReport.LineDivergence.DivergenceType determineDivergenceType(
            String actual, String expected, EquivalenceConfig config) {
        
        // Check if it's a whitespace difference
        if (config.ignoreWhitespace() && actual.trim().equals(expected.trim())) {
            return EquivalenceReport.LineDivergence.DivergenceType.WHITESPACE;
        }
        
        // Check if it's a metadata difference
        for (String pattern : config.excludedMetadataPatterns()) {
            if (actual.contains(pattern) || expected.contains(pattern)) {
                return EquivalenceReport.LineDivergence.DivergenceType.METADATA;
            }
        }
        
        // Default to content difference
        return EquivalenceReport.LineDivergence.DivergenceType.CONTENT;
    }
}
