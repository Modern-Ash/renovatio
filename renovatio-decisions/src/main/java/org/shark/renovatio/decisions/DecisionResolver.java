package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.shark.renovatio.profile.TemplateReference;

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
        Map<String, String> accepted = new LinkedHashMap<>();
        List<String> applied = new ArrayList<>();
        decisions.stream().filter(DecisionPoint::active)
                .filter(value -> value.status() == CONFIRMED || value.status() == OVERRIDDEN)
                .sorted(Comparator.comparing(DecisionPoint::id)).forEach(value -> {
                    if (values.put(value.decisionKey(), value.chosenOption()) == null)
                        throw new IllegalStateException("Unknown F1 decision key: " + value.decisionKey());
                    accepted.put(value.decisionKey(), value.chosenOption());
                    applied.add(value.id());
                });
        return MigrationProfiles.effective(overlay, values, accepted, applied);
    }

    /** Resolves the F8 layers and binds exact reusable-asset versions into the effective hash. */
    public MigrationProfiles.EffectiveProfile resolve(MigrationProfile template,
                                                      TemplateReference templateReference,
                                                      List<DecisionPoint> policyDecisions,
                                                      PolicyReference policyReference,
                                                      MigrationProfile projectOverlay,
                                                      List<DecisionPoint> projectDecisions) {
        List<DecisionPoint> all = new ArrayList<>();
        if (policyDecisions != null) all.addAll(policyDecisions);
        if (projectDecisions != null) all.addAll(projectDecisions);
        ensureUnique(all);

        Map<String, String> values = new LinkedHashMap<>();
        F1DecisionCatalog.definitions().forEach((key, definition) -> values.put(key, definition.options().get(0)));
        Map<String, String> policyAccepted = accepted(policyDecisions, values);
        Map<String, String> projectAccepted = accepted(projectDecisions, values);
        List<String> applied = all.stream().filter(DecisionPoint::active)
                .filter(value -> value.status() == CONFIRMED || value.status() == OVERRIDDEN)
                .map(DecisionPoint::id).distinct().sorted().toList();
        Map<String, String> bindings = new LinkedHashMap<>();
        if (templateReference != null) bindings.put("template", templateReference.name() + "@" + templateReference.version());
        if (policyReference != null) bindings.put("policyCatalog", policyReference.name() + "@" + policyReference.version());
        return MigrationProfiles.effectiveLayers(template, policyAccepted, projectOverlay, values,
                projectAccepted, applied, bindings);
    }

    private static Map<String, String> accepted(List<DecisionPoint> decisions, Map<String, String> values) {
        Map<String, String> accepted = new LinkedHashMap<>();
        if (decisions == null) return accepted;
        decisions.stream().filter(DecisionPoint::active)
                .filter(value -> value.status() == CONFIRMED || value.status() == OVERRIDDEN)
                .sorted(Comparator.comparing(DecisionPoint::id)).forEach(value -> {
                    if (values.put(value.decisionKey(), value.chosenOption()) == null)
                        throw new IllegalStateException("Unknown F1 decision key: " + value.decisionKey());
                    accepted.put(value.decisionKey(), value.chosenOption());
                });
        return accepted;
    }

    private static void ensureUnique(List<DecisionPoint> decisions) {
        HashSet<String> identities = new HashSet<>();
        decisions.stream().filter(DecisionPoint::active).forEach(value -> {
            String identity = value.decisionKey() + "\u0000" + value.location().programId()
                    + "\u0000" + value.location().nodeKind() + "\u0000" + value.location().nodeId();
            if (!identities.add(identity)) throw new IllegalStateException("Duplicate active decision: " + value.decisionKey());
        });
    }

    public static Comparator<DecisionPoint> apiOrder() {
        return Comparator.comparing(DecisionPoint::category)
                .thenComparing(DecisionPoint::decisionKey)
                .thenComparing(value -> value.location().programId())
                .thenComparing(value -> value.location().nodeId())
                .thenComparing(DecisionPoint::id);
    }
}
