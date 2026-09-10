package org.shark.renovatio.provider.cobol.service.generation;

import org.shark.renovatio.provider.cobol.domain.CobolProgram;
import org.shark.renovatio.provider.cobol.service.CobolParsingService;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for parsing COBOL programs into intermediate representation.
 * This is the first stage of the generation pipeline.
 */
@Service
public class JavaParseService {

    private static final Logger log = LoggerFactory.getLogger(JavaParseService.class);

    private final CobolParsingService parsingService;

    public JavaParseService(CobolParsingService parsingService) {
        this.parsingService = parsingService;
    }

    /**
     * Parse COBOL programs from a query.
     *
     * @param query the NQL query
     * @param workspace the workspace
     * @return list of parsed COBOL programs
     * @throws ParsingException if parsing fails
     */
    public List<CobolProgram> parse(NqlQuery query, Workspace workspace) throws ParsingException {
        log.debug("Parsing COBOL programs for query: {}", query);

        try {
            var analyzeResult = parsingService.analyzeCOBOL(query, workspace);
            if (!analyzeResult.isSuccess()) {
                throw new ParsingException("Failed to analyze COBOL: " + analyzeResult.getMessage());
            }

            @SuppressWarnings("unchecked")
            List<CobolProgram> programs = (List<CobolProgram>)
                    ((Map<String, Object>) analyzeResult.getData()).get("programs");

            if (programs == null) {
                programs = Collections.emptyList();
            }

            log.debug("Parsed {} COBOL programs", programs.size());
            return programs;
        } catch (Exception e) {
            throw new ParsingException("Error parsing COBOL programs: " + e.getMessage(), e);
        }
    }

    /**
     * Extract metadata from a parsed program.
     *
     * @param program the parsed COBOL program
     * @return program metadata
     */
    public Map<String, Object> extractMetadata(CobolProgram program) {
        if (program == null) {
            return Collections.emptyMap();
        }
        return program.getMetadata() != null ? program.getMetadata() : Collections.emptyMap();
    }

    /**
     * Exception thrown when parsing fails.
     */
    public static class ParsingException extends Exception {
        public ParsingException(String message) {
            super(message);
        }

        public ParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}