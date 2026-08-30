package org.shark.renovatio.provider.cobol.service;

import org.shark.renovatio.shared.nql.NqlParserService;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * COBOL-provider facade for parsing NQL queries with stable fallback behavior.
 */
@Service
public class CobolNqlParsingService {

    private static final Logger logger = LoggerFactory.getLogger(CobolNqlParsingService.class);

    private final NqlParserService nqlParserService;

    public CobolNqlParsingService(NqlParserService nqlParserService) {
        this.nqlParserService = nqlParserService;
    }

    /**
     * Parses an NQL query and returns a type-less query when parsing fails.
     *
     * @param nqlString query text
     * @return parsed query or stable fallback
     */
    public NqlQuery parseNqlQuery(String nqlString) {
        logger.debug("Parsing NQL query");
        try {
            NqlQuery query = nqlParserService.parse(nqlString);
            if (query != null && query.getType() != null) {
                return query;
            }
            return fallback(nqlString);
        } catch (RuntimeException exception) {
            logger.warn("NQL parsing failed: {}", exception.getClass().getSimpleName());
            return fallback(nqlString);
        }
    }

    private static NqlQuery fallback(String nqlString) {
        NqlQuery fallbackQuery = new NqlQuery();
        fallbackQuery.setOriginalQuery(nqlString);
        fallbackQuery.setType(null);
        return fallbackQuery;
    }
}
