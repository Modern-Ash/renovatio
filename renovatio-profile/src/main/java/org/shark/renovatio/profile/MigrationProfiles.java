package org.shark.renovatio.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.shark.renovatio.profile.MigrationProfile.*;

/** Deterministic profile codec, validation, overlay resolution, and effective hashing. */
public final class MigrationProfiles {
    public static final String SCHEMA_VERSION = "1";
    private static final ObjectMapper JSON = mapper(new ObjectMapper());
    private static final ObjectMapper YAML = mapper(new ObjectMapper(new YAMLFactory()));

    private MigrationProfiles() { }

    private static ObjectMapper mapper(ObjectMapper mapper) {
        return mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public static MigrationProfile defaults() {
        return new MigrationProfile(SCHEMA_VERSION, Map.of(),
                new Target(Language.JAVA, "17"),
                new Architecture(ArchitectureStyle.TRANSACTION_SCRIPT, ModuleGrouping.BY_PROGRAM),
                new MigrationProfile.Runtime(Framework.SPRING_BOOT),
                new Persistence(PersistenceStrategy.IN_MEMORY, TransactionBoundary.METHOD),
                new Style(NumericPolicy.BIGDECIMAL, Nullability.NON_NULL_BY_DEFAULT,
                        ErrorHandling.EXCEPTIONS, Naming.JAVA_BEANS),
                new Llm(false, false, 0));
    }

    public static MigrationProfile emptyOverlay() {
        return new MigrationProfile(SCHEMA_VERSION, Map.of(), null, null, null, null, null, null);
    }

    public static MigrationProfile readJson(String value) {
        return read(JSON, value);
    }

    public static MigrationProfile readYaml(String value) {
        return read(YAML, value);
    }

    private static MigrationProfile read(ObjectMapper mapper, String value) {
        try {
            MigrationProfile profile = mapper.readValue(value, MigrationProfile.class);
            List<Violation> violations = validateOverlay(profile);
            if (!violations.isEmpty()) throw new ProfileValidationException(violations);
            return profile;
        } catch (ProfileValidationException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new ProfileFormatException("Malformed migration profile", exception);
        }
    }

    public static String writeJson(MigrationProfile profile) {
        try {
            return JSON.writeValueAsString(profile);
        } catch (JsonProcessingException exception) {
            throw new ProfileFormatException("Cannot serialize migration profile", exception);
        }
    }

    public static String writeYaml(MigrationProfile profile) {
        try {
            return YAML.writeValueAsString(profile);
        } catch (JsonProcessingException exception) {
            throw new ProfileFormatException("Cannot serialize migration profile", exception);
        }
    }

    public static MigrationProfile resolve(MigrationProfile overlay) {
        List<Violation> violations = validateOverlay(overlay);
        if (!violations.isEmpty()) throw new ProfileValidationException(violations);
        MigrationProfile base = defaults();
        return new MigrationProfile(SCHEMA_VERSION,
                overlay.extensions(),
                merge(base.target(), overlay.target()),
                merge(base.architecture(), overlay.architecture()),
                merge(base.runtime(), overlay.runtime()),
                merge(base.persistence(), overlay.persistence()),
                merge(base.style(), overlay.style()),
                merge(base.llm(), overlay.llm()));
    }

    private static Target merge(Target base, Target value) {
        return value == null ? base : new Target(value.language() == null ? base.language() : value.language(),
                value.languageVersion() == null ? base.languageVersion() : value.languageVersion());
    }

    private static Architecture merge(Architecture base, Architecture value) {
        return value == null ? base : new Architecture(value.style() == null ? base.style() : value.style(),
                value.moduleGrouping() == null ? base.moduleGrouping() : value.moduleGrouping());
    }

    private static MigrationProfile.Runtime merge(MigrationProfile.Runtime base, MigrationProfile.Runtime value) {
        return value == null ? base : new MigrationProfile.Runtime(
                value.framework() == null ? base.framework() : value.framework());
    }

    private static Persistence merge(Persistence base, Persistence value) {
        return value == null ? base : new Persistence(
                value.defaultStrategy() == null ? base.defaultStrategy() : value.defaultStrategy(),
                value.transactionBoundary() == null ? base.transactionBoundary() : value.transactionBoundary());
    }

    private static Style merge(Style base, Style value) {
        return value == null ? base : new Style(
                value.numericPolicy() == null ? base.numericPolicy() : value.numericPolicy(),
                value.nullability() == null ? base.nullability() : value.nullability(),
                value.errorHandling() == null ? base.errorHandling() : value.errorHandling(),
                value.naming() == null ? base.naming() : value.naming());
    }

    private static Llm merge(Llm base, Llm value) {
        return value == null ? base : new Llm(value.enabled() == null ? base.enabled() : value.enabled(),
                value.suggestDecisions() == null ? base.suggestDecisions() : value.suggestDecisions(),
                value.maxSuggestionsPerRun() == null ? base.maxSuggestionsPerRun() : value.maxSuggestionsPerRun());
    }

    public static List<Violation> validateOverlay(MigrationProfile profile) {
        List<Violation> result = new ArrayList<>();
        if (profile == null) return List.of(new Violation("/", "REQUIRED", "profile is required"));
        if (!SCHEMA_VERSION.equals(profile.schemaVersion()))
            result.add(new Violation("/schemaVersion", "UNSUPPORTED_VERSION", "must equal 1"));
        if (profile.extensions() == null)
            result.add(new Violation("/extensions", "REQUIRED", "is required"));
        if (profile.target() != null && profile.target().languageVersion() != null) {
            String version = profile.target().languageVersion();
            if (version.isBlank() || version.length() > 32)
                result.add(new Violation("/target/languageVersion", "INVALID_LENGTH", "must be non-blank and at most 32 characters"));
        }
        MigrationProfile effective = profile.schemaVersion() == null || profile.extensions() == null
                ? null : resolveWithoutValidation(profile);
        if (effective != null) {
            Llm llm = effective.llm();
            if (llm.maxSuggestionsPerRun() < 0 || llm.maxSuggestionsPerRun() > 100)
                result.add(new Violation("/llm/maxSuggestionsPerRun", "OUT_OF_RANGE", "must be between 0 and 100"));
            if (llm.suggestDecisions() && !llm.enabled())
                result.add(new Violation("/llm/suggestDecisions", "REQUIRES_ENABLED", "requires llm.enabled=true"));
            if (llm.suggestDecisions() && llm.maxSuggestionsPerRun() < 1)
                result.add(new Violation("/llm/maxSuggestionsPerRun", "REQUIRES_POSITIVE_CAP", "must be between 1 and 100 when suggestions are enabled"));
            if (!llm.suggestDecisions() && llm.maxSuggestionsPerRun() != 0)
                result.add(new Violation("/llm/maxSuggestionsPerRun", "REQUIRES_ZERO_CAP", "must be 0 when suggestions are disabled"));
        }
        result.sort(Comparator.comparing(Violation::path).thenComparing(Violation::code));
        return List.copyOf(result);
    }

    private static MigrationProfile resolveWithoutValidation(MigrationProfile overlay) {
        MigrationProfile base = defaults();
        return new MigrationProfile(SCHEMA_VERSION, overlay.extensions(), merge(base.target(), overlay.target()),
                merge(base.architecture(), overlay.architecture()), merge(base.runtime(), overlay.runtime()),
                merge(base.persistence(), overlay.persistence()), merge(base.style(), overlay.style()),
                merge(base.llm(), overlay.llm()));
    }

    public static EffectiveProfile effective(MigrationProfile overlay, Map<String, String> decisions,
                                             List<String> appliedDecisionIds) {
        MigrationProfile profile = resolve(overlay);
        Map<String, String> ordered = new java.util.TreeMap<>(decisions == null ? Map.of() : decisions);
        if ("FLUENT".equals(ordered.get("java.accessor-convention"))) {
            profile = withNaming(profile, Naming.FLUENT);
        } else if ("JAVA_BEANS".equals(ordered.get("java.accessor-convention"))) {
            profile = withNaming(profile, Naming.JAVA_BEANS);
        }
        if ("PLAIN_JAVA".equals(ordered.get("java.framework-coupling"))) {
            profile = withFramework(profile, Framework.NONE);
        } else if ("SPRING_SERVICE".equals(ordered.get("java.framework-coupling"))) {
            profile = withFramework(profile, Framework.SPRING_BOOT);
        }
        List<String> ids = appliedDecisionIds == null ? List.of() : appliedDecisionIds.stream().sorted().toList();
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("profile", JSON.convertValue(profile, Object.class));
        projection.put("resolvedDecisions", ordered);
        projection.put("appliedDecisionIds", ids);
        return new EffectiveProfile(profile, ordered, ids, sha256(canonical(projection)));
    }

    private static MigrationProfile withNaming(MigrationProfile value, Naming naming) {
        Style style = value.style();
        return new MigrationProfile(value.schemaVersion(), value.extensions(), value.target(), value.architecture(),
                value.runtime(), value.persistence(), new Style(style.numericPolicy(), style.nullability(),
                style.errorHandling(), naming), value.llm());
    }

    private static MigrationProfile withFramework(MigrationProfile value, Framework framework) {
        return new MigrationProfile(value.schemaVersion(), value.extensions(), value.target(), value.architecture(),
                new MigrationProfile.Runtime(framework), value.persistence(), value.style(), value.llm());
    }

    public static String canonical(Object value) {
        return appendCanonical(new StringBuilder(), value).toString();
    }

    @SuppressWarnings("unchecked")
    private static StringBuilder appendCanonical(StringBuilder out, Object raw) {
        Object value = raw instanceof JsonNode node ? JSON.convertValue(node, Object.class) : raw;
        if (value == null) return out.append("null");
        if (value instanceof String string) return string(out, Normalizer.normalize(string, Normalizer.Form.NFC));
        if (value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long || value instanceof BigInteger)
            return out.append(value);
        if (value instanceof Number number) {
            BigDecimal decimal = new BigDecimal(number.toString()).stripTrailingZeros();
            return out.append(decimal.signum() == 0 ? "0" : decimal.toPlainString());
        }
        if (value instanceof Enum<?> enumeration) return string(out, enumeration.name());
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));
            for (int index = 0; index < entries.size(); index++) {
                if (index > 0) out.append(',');
                string(out, String.valueOf(entries.get(index).getKey())).append(':');
                appendCanonical(out, entries.get(index).getValue());
            }
            return out.append('}');
        }
        if (value instanceof Iterable<?> iterable) {
            out.append('['); boolean separator = false;
            for (Object item : iterable) { if (separator) out.append(','); appendCanonical(out, item); separator = true; }
            return out.append(']');
        }
        return appendCanonical(out, JSON.convertValue(value, Object.class));
    }

    private static StringBuilder string(StringBuilder out, String value) {
        out.append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (current <= 0x1f) out.append(String.format("\\u%04x", (int) current)); else out.append(current); }
            }
        }
        return out.append('"');
    }

    public static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public record Violation(String path, String code, String message) { }
    public record EffectiveProfile(MigrationProfile profile, Map<String, String> resolvedDecisions,
                                   List<String> appliedDecisionIds, String profileHash) { }

    public static final class ProfileValidationException extends IllegalArgumentException {
        private final List<Violation> violations;
        public ProfileValidationException(List<Violation> violations) {
            super("Migration profile validation failed");
            this.violations = List.copyOf(violations);
        }
        public List<Violation> violations() { return violations; }
    }

    public static final class ProfileFormatException extends IllegalArgumentException {
        public ProfileFormatException(String message, Throwable cause) { super(message, cause); }
    }
}
