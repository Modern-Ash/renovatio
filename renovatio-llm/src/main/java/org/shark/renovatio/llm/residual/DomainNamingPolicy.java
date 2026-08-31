package org.shark.renovatio.llm.residual;

import javax.lang.model.SourceVersion;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Deterministic eligibility policy for model-proposed Java/domain names. */
public final class DomainNamingPolicy {

    public Decision validate(String suggestedName, List<String> collisionScope,
                             boolean publicSignatureProtected) {
        String normalized = normalize(suggestedName);
        if (!SourceVersion.isIdentifier(normalized) || SourceVersion.isKeyword(normalized)) {
            throw new IllegalArgumentException("suggested name is not a legal non-keyword Java identifier");
        }
        String comparison = normalized.toLowerCase(Locale.ROOT);
        boolean collision = List.copyOf(Objects.requireNonNull(collisionScope, "collisionScope")).stream()
                .map(DomainNamingPolicy::normalize)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(comparison::equals);
        if (collision) throw new IllegalArgumentException("suggested name collides in the governed program scope");
        return new Decision(normalized, publicSignatureProtected, false);
    }

    /** Matches the deterministic COBOL-to-Java camel-case convention used by the IR parser. */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("suggestedName must not be blank");
        String[] parts = value.trim().split("[-_\\s]+", -1);
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String lower = part.toLowerCase(Locale.ROOT);
            if (result.isEmpty()) result.append(lower);
            else result.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("suggestedName normalizes to empty");
        return result.toString();
    }

    /** Domain proposals are review-only, including unprotected internal symbols. */
    public record Decision(String normalizedName, boolean publicSignatureProtected,
                           boolean autoApplicable) {
    }
}
