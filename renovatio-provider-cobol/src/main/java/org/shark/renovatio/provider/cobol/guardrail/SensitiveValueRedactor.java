package org.shark.renovatio.provider.cobol.guardrail;

import java.util.List;
import java.util.regex.Pattern;

/** Removes common credential forms before diagnostic data is persisted or uploaded. */
final class SensitiveValueRedactor {

    private static final String REDACTED = "[REDACTED]";
    private static final List<Pattern> SECRETS = List.of(
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*)(?:bearer\\s+)?[^\\s,;]+"),
            Pattern.compile("(?i)((?:api[-_ ]?key|token|secret|password)\\s*[:=]\\s*)[^\\s,;]+"),
            Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+"),
            Pattern.compile("(?<![A-Za-z0-9])sk-[A-Za-z0-9_-]{8,}"));

    String redact(String value) {
        String redacted = value;
        for (Pattern pattern : SECRETS) {
            redacted = pattern.matcher(redacted).replaceAll(match ->
                    match.groupCount() == 0 ? REDACTED : match.group(1) + REDACTED);
        }
        return redacted;
    }
}
