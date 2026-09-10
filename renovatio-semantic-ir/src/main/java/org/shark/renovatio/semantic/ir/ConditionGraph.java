package org.shark.renovatio.semantic.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Ordered guards controlling whether batch steps run. */
public record ConditionGraph(List<Guard> guards) {
    public ConditionGraph {
        guards = guards == null ? List.of() : List.copyOf(guards);
    }

    public record Guard(String predicate, Optional<String> referencedStepId,
                        Map<String, Boolean> truthTable, List<String> memberStepIds) {
        public Guard {
            predicate = SemanticIdentity.text(predicate, "predicate");
            referencedStepId = referencedStepId == null ? Optional.empty()
                    : referencedStepId.map(value -> SemanticIdentity.hash(value, "referencedStepId"));
            truthTable = truthTable == null ? Map.of()
                    : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(truthTable));
            if (truthTable.isEmpty()) throw new IllegalArgumentException("truthTable must not be empty");
            memberStepIds = memberStepIds == null ? List.of() : List.copyOf(memberStepIds);
            if (memberStepIds.isEmpty()) throw new IllegalArgumentException("memberStepIds must not be empty");
            memberStepIds.forEach(value -> SemanticIdentity.hash(value, "memberStepId"));
        }
    }
}
