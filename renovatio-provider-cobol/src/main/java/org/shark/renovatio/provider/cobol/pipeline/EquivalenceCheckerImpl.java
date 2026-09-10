package org.shark.renovatio.provider.cobol.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            // Find all Java files in actual directory
            List<Path> actualFiles = findJavaFiles(actualDir);
            
            for (Path actualFile : actualFiles) {
                // Find corresponding expected file
                Path relativePath = actualDir.relativize(actualFile);
                Path expectedFile = expectedDir.resolve(relativePath);
                
                if (Files.exists(expectedFile)) {
                    EquivalenceReport report = check(actualFile, expectedFile, config);
                    reports.add(report);
                } else {
                    log.warn("No expected file found for: {}", actualFile);
                }
            }
            
        } catch (IOException e) {
            log.error("Error finding Java files for equivalence check", e);
        }
        
        return reports;
    }
    
    private List<Path> findJavaFiles(Path directory) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.filter(path -> path.toString().endsWith(".java"))
                      .forEach(javaFiles::add);
            }
        }
        
        return javaFiles;
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
