package org.shark.renovatio.emitter.node.catalog;

import java.util.List;
import java.util.Map;

public final class NodeIdiomCatalog {
    private static final Map<String, String> IDIOMS = Map.ofEntries(
            Map.entry("MOVE", "assignment"),
            Map.entry("COMPUTE", "expression"),
            Map.entry("IF", "if"),
            Map.entry("EVALUATE", "switch"),
            Map.entry("PERFORM", "function call"),
            Map.entry("READ", "await fs.readFile()"),
            Map.entry("WRITE", "await fs.writeFile()"),
            Map.entry("EXEC SQL", "await prisma.model.findMany()"),
            Map.entry("DISPLAY", "console.log"),
            Map.entry("ACCEPT", "req.body / req.params")
    );

    public String idiomFor(String cobolConstruct) {
        return IDIOMS.getOrDefault(cobolConstruct, "manual action item");
    }

    public Map<String, String> allIdioms() {
        return Map.copyOf(IDIOMS);
    }

    public List<String> missingPatterns() {
        return IDIOMS.entrySet().stream()
                .filter(e -> e.getValue().startsWith("manual"))
                .map(Map.Entry::getKey)
                .toList();
    }
}
