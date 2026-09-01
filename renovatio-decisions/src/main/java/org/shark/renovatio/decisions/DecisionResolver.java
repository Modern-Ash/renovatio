package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.shark.renovatio.decisions.DecisionPoint.Status.CONFIRMED;
import static org.shark.renovatio.decisions.DecisionPoint.Status.OVERRIDDEN;

/** Resolves defaults plus accepted project decisions into one content-addressed envelope. */
public final class DecisionResolver {
    public MigrationProfiles.EffectiveProfile resolve(MigrationProfile overlay, List<DecisionPoint> decisions) {
        HashSet<String> identities = new HashSet<>();
        decisions.stream().filter(DecisionPoint::active).forEach(value -> {
            String identity = value.decisionKey() + "\u0000" + value.location().programId()
                    + "\u0000" + value.location().nodeKind() + "\u0000" + value.location().nodeId();
            if (!identities.add(identity)) throw new IllegalStateException("Duplicate active decision: " + value.decisionKey());
        });
        Map<String, String> values = new LinkedHashMap<>();
        F1DecisionCatalog.definitions().forEach((key, definition) -> values.put(key, definition.options().get(0)));
        List<String> applied = new ArrayList<>();
        decisions.stream().filter(DecisionPoint::active)
                .filter(value -> value.status() == CONFIRMED || value.status() == OVERRIDDEN)
                .sorted(Comparator.comparing(DecisionPoint::id)).forEach(value -> {
                    if (values.put(value.decisionKey(), value.chosenOption()) == null)
                        throw new IllegalStateException("Unknown F1 decision key: " + value.decisionKey());
                    applied.add(value.id());
                });
        return MigrationProfiles.effective(overlay, values, applied);
    }

    public static Comparator<DecisionPoint> apiOrder() {
        return Comparator.comparing(DecisionPoint::category)
                .thenComparing(DecisionPoint::decisionKey)
                .thenComparing(value -> value.location().programId())
                .thenComparing(value -> value.location().nodeId())
                .thenComparing(DecisionPoint::id);
    }
}
