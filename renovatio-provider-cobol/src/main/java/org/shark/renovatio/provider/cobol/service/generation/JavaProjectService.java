package org.shark.renovatio.provider.cobol.service.generation;

import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for projecting COBOL data structures to Java models.
 * This is the second stage of the generation pipeline.
 */
@Service
public class JavaProjectService {

    private static final Logger log = LoggerFactory.getLogger(JavaProjectService.class);

    private final CobolIntermediateModelService intermediateModelService;

    public JavaProjectService(CobolIntermediateModelService intermediateModelService) {
        this.intermediateModelService = intermediateModelService;
    }

    /**
     * Project COBOL metadata to a list of field definitions.
     *
     * @param metadata the COBOL program metadata
     * @return list of field definitions
     */
    public List<FieldDefinition> projectFields(Map<String, Object> metadata) {
        log.debug("Projecting COBOL fields to Java");

        if (metadata == null) {
            return Collections.emptyList();
        }

        // Check if there are ENTRY points - if so, use linkageItems instead of dataItems
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) metadata.get("entries");
        List<Map<String, Object>> dataItems;

        if (entries != null && !entries.isEmpty()) {
            // Use linkage section items for programs with ENTRY points
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> linkageItems = (List<Map<String, Object>>) metadata.get("linkageItems");
            dataItems = linkageItems != null ? linkageItems : Collections.emptyList();
            log.debug("Using linkageItems for projection, count: {}", dataItems.size());
        } else {
            // Use working-storage items for regular programs
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> wsItems = (List<Map<String, Object>>) metadata.get("dataItems");
            dataItems = wsItems != null ? wsItems : Collections.emptyList();
            log.debug("Using dataItems for projection, count: {}", dataItems.size());
        }

        return dataItems.stream()
                .filter(item -> item.get("name") != null && item.get("javaType") != null)
                .map(item -> new FieldDefinition(
                        (String) item.get("name"),
                        (String) item.get("javaType"),
                        extractMaxLength(item),
                        extractPrecision(item),
                        extractScale(item),
                        extractAllowsNegative(item)
                ))
                .toList();
    }

    /**
     * Resolve the intermediate model for a program.
     *
     * @param metadata the COBOL program metadata
     * @return the intermediate model
     */
    public CobolIntermediateModel resolveIntermediateModel(Map<String, Object> metadata) {
        log.debug("Resolving intermediate model");
        // Extract COBOL source from metadata if available
        String cobolSource = (String) metadata.get("source");
        if (cobolSource != null) {
            return intermediateModelService.parse(cobolSource);
        }
        // Return null if no source available
        return null;
    }

    /**
     * Extract ENTRY points from metadata.
     *
     * @param metadata the COBOL program metadata
     * @return list of entry points
     */
    public List<Map<String, Object>> extractEntryPoints(Map<String, Object> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) metadata.get("entries");
        return entries != null ? entries : Collections.emptyList();
    }

    private Integer extractMaxLength(Map<String, Object> item) {
        Object maxLength = item.get("maxLength");
        if (maxLength instanceof Integer) {
            return (Integer) maxLength;
        }
        return null;
    }

    private Integer extractPrecision(Map<String, Object> item) {
        Object precision = item.get("precision");
        if (precision instanceof Integer) {
            return (Integer) precision;
        }
        return null;
    }

    private Integer extractScale(Map<String, Object> item) {
        Object scale = item.get("scale");
        if (scale instanceof Integer) {
            return (Integer) scale;
        }
        return null;
    }

    private boolean extractAllowsNegative(Map<String, Object> item) {
        Object allowsNegative = item.get("allowsNegative");
        if (allowsNegative instanceof Boolean) {
            return (Boolean) allowsNegative;
        }
        return false;
    }

    /**
     * Record representing a projected field definition.
     */
    public record FieldDefinition(
            String name,
            String javaType,
            Integer maxLength,
            Integer precision,
            Integer scale,
            boolean allowsNegative
    ) {
    }
}