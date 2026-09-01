package org.shark.renovatio.decisions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.shark.renovatio.decisions.DecisionPoint.*;

/** The complete, ordered seven-decision policy admitted by F0. */
public final class F1DecisionCatalog {
    private static final Map<String, Definition> DEFINITIONS = buildDefinitions();

    private F1DecisionCatalog() { }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    public static List<DecisionPoint> create(String semanticIrHash, Instant now) {
        Location location = Location.project();
        return DEFINITIONS.values().stream().map(definition -> new DecisionPoint(
                DecisionPoint.SCHEMA_VERSION,
                DecisionIdentity.id(definition.category(), definition.key(), location),
                definition.category(), definition.key(), location, definition.question(),
                definition.options(), definition.options().get(0), definition.options().get(0),
                Source.HEURISTIC, BigDecimal.ONE, definition.rationale(), definition.evidence(),
                Status.AUTO, semanticIrHash, false, null, 1, now, now, true)).toList();
    }

    private static Map<String, Definition> buildDefinitions() {
        Map<String, Definition> result = new LinkedHashMap<>();
        add(result, Category.NUMERIC, "java.numeric.unscaled-type",
                "Which Java type policy should unscaled numeric PIC items use?",
                List.of("CURRENT_PIC_MAPPING", "ALWAYS_LONG", "BIG_INTEGER"), "F0 #1");
        add(result, Category.NAMING, "java.naming.identifier-mapping",
                "How should COBOL identifiers map to Java identifiers?",
                List.of("CANONICAL_JAVA_IDENTIFIER", "PRESERVE_SANITIZED_IDENTIFIER"), "F0 #27");
        add(result, Category.NAMING, "java.generated-package",
                "Which package should contain generated Java?",
                List.of("org.shark.renovatio.generated.cobol", "org.shark.renovatio.generated"), "F0 #28");
        add(result, Category.NAMING, "java.accessor-convention",
                "Which Java accessor convention should generated models expose?",
                List.of("JAVA_BEANS", "FLUENT"), "F0 #30");
        add(result, Category.ARCHITECTURE, "java.framework-coupling",
                "Should generated services retain Spring coupling?",
                List.of("SPRING_SERVICE", "PLAIN_JAVA"), "F0 #33");
        add(result, Category.NUMERIC, "cobol.pic.default-usage",
                "Which COBOL usage applies when PIC omits an explicit usage?",
                List.of("DISPLAY", "COMP", "COMP_3"), "F0 #37");
        add(result, Category.DATA_SHAPE, "java.value-initializer-policy",
                "How should COBOL VALUE clauses be represented in Java?",
                List.of("DROP_INITIAL_VALUE", "FIELD_INITIALIZER", "CONSTRUCTOR_INITIALIZER"), "F0 #38");
        return java.util.Collections.unmodifiableMap(result);
    }

    private static void add(Map<String, Definition> target, Category category, String key,
                            String question, List<String> options, String evidence) {
        target.put(key, new Definition(category, key, question, List.copyOf(options),
                "Deterministic compatibility default from " + evidence, List.of(evidence)));
    }

    public record Definition(Category category, String key, String question, List<String> options,
                             String rationale, List<String> evidence) { }
}
