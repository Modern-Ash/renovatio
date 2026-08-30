package org.shark.renovatio.llm.prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/** Immutable prompt registry preserving catalog order. */
public final class PromptCatalog {
    private final Map<String, PromptDefinition> entries;

    PromptCatalog(List<PromptDefinition> definitions) {
        LinkedHashMap<String, PromptDefinition> indexed = new LinkedHashMap<>();
        definitions.forEach(definition -> indexed.put(definition.promptId(), definition));
        this.entries = Collections.unmodifiableMap(indexed);
    }

    public PromptDefinition require(String promptId) {
        PromptDefinition definition = entries.get(promptId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown promptId: " + promptId);
        }
        return definition;
    }

    public List<PromptDefinition> entries() {
        return List.copyOf(entries.values());
    }
}
