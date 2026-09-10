package org.shark.renovatio.provider.cobol.service.generation;

import org.shark.renovatio.shared.domain.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for writing generated Java code to disk.
 * This is the fifth stage of the generation pipeline.
 */
@Service
public class JavaWriteService {

    private static final Logger log = LoggerFactory.getLogger(JavaWriteService.class);

    /**
     * Write generated files to disk.
     *
     * @param generatedFiles map of filename to content
     * @param workspace the workspace
     * @return the output path where files were written
     * @throws WriteException if writing fails
     */
    public String writeFiles(Map<String, String> generatedFiles, Workspace workspace) throws WriteException {
        if (generatedFiles == null || generatedFiles.isEmpty()) {
            log.warn("No files to write");
            return "";
        }

        try {
            Path outputPath = resolveOutputPath(workspace);
            Files.createDirectories(outputPath);

            for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();

                Path filePath = outputPath.resolve(filename);
                Files.writeString(filePath, content);

                log.debug("Written file: {}", filePath);
            }

            String outputPathStr = outputPath.toString();
            log.info("Written {} files to: {}", generatedFiles.size(), outputPathStr);
            return outputPathStr;
        } catch (IOException e) {
            throw new WriteException("Failed to write generated files: " + e.getMessage(), e);
        }
    }

    /**
     * Resolve the output path for generated files.
     *
     * @param workspace the workspace
     * @return the resolved output path
     */
    private Path resolveOutputPath(Workspace workspace) {
        if (workspace != null && workspace.getPath() != null) {
            return Paths.get(workspace.getPath(), "generated-java-stubs");
        }
        return Paths.get("generated-java-stubs");
    }

    /**
     * Exception thrown when writing fails.
     */
    public static class WriteException extends Exception {
        public WriteException(String message) {
            super(message);
        }

        public WriteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}