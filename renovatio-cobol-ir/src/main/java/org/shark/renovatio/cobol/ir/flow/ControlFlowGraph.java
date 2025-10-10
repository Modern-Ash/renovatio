package org.shark.renovatio.cobol.ir.flow;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ControlFlowGraph {

    private final Map<String, Set<String>> adjacency;

    private ControlFlowGraph(Map<String, Set<String>> adjacency) {
        this.adjacency = Collections.unmodifiableMap(adjacency);
    }

    public Map<String, Set<String>> getAdjacency() {
        return adjacency;
    }

    public Set<String> successors(String node) {
        if (node == null) {
            return Set.of();
        }
        return adjacency.getOrDefault(node.toUpperCase(), Set.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ControlFlowGraph empty() {
        return new ControlFlowGraph(Map.of());
    }

    public static final class Builder {
        private final Map<String, Set<String>> adjacency = new LinkedHashMap<>();

        public Builder addEdge(String from, String to) {
            if (from == null || to == null) {
                return this;
            }
            adjacency.computeIfAbsent(from.toUpperCase(), k -> new LinkedHashSet<>())
                    .add(to.toUpperCase());
            adjacency.putIfAbsent(to.toUpperCase(), new LinkedHashSet<>());
            return this;
        }

        public Builder ensureNode(String node) {
            if (node != null) {
                adjacency.putIfAbsent(node.toUpperCase(), new LinkedHashSet<>());
            }
            return this;
        }

        public ControlFlowGraph build() {
            Map<String, Set<String>> copy = new LinkedHashMap<>();
            adjacency.forEach((k, v) -> copy.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(v))));
            return new ControlFlowGraph(copy);
        }
    }
}
