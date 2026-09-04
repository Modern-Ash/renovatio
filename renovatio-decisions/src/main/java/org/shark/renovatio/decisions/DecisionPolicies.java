package org.shark.renovatio.decisions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure export and guarded matching operations for reusable policy catalogs. */
public final class DecisionPolicies {
    public static final BigDecimal DEFAULT_AUTO_CONFIRM = new BigDecimal("0.95");
    public static final BigDecimal DEFAULT_SUGGEST = new BigDecimal("0.75");

    private DecisionPolicies() { }

    public static DecisionPolicyCatalog exportCatalog(String name, String version, String projectId,
                                                       String analyzerVersion, List<DecisionPoint> decisions,
                                                       Map<String, Map<String, String>> featuresByDecision,
                                                       BigDecimal autoThreshold, BigDecimal suggestThreshold,
                                                       Instant now) {
        Map<String, DecisionPolicyEntry> bySignature = new LinkedHashMap<>();
        decisions.stream().filter(DecisionPoint::active)
                .filter(value -> value.status() == DecisionPoint.Status.CONFIRMED
                        || value.status() == DecisionPoint.Status.OVERRIDDEN)
                .sorted(Comparator.comparing(DecisionPoint::id)).forEach(decision -> {
                    var signature = SemanticDecisionSignature.create(decision, analyzerVersion,
                            featuresByDecision == null ? Map.of() : featuresByDecision.getOrDefault(decision.id(), Map.of()));
                    var entry = DecisionPolicyEntry.from(projectId, decision, signature);
                    var previous = bySignature.putIfAbsent(signature.digest(), entry);
                    if (previous != null && !previous.chosenOption().equals(entry.chosenOption())) {
                        throw new PolicyConflictException("Conflicting choices for semantic signature " + signature.digest());
                    }
                });
        return DecisionPolicyCatalog.create(name, version, analyzerVersion,
                autoThreshold == null ? DEFAULT_AUTO_CONFIRM : autoThreshold,
                suggestThreshold == null ? DEFAULT_SUGGEST : suggestThreshold,
                List.copyOf(bySignature.values()), now);
    }

    public static ApplyResult apply(DecisionPolicyCatalog catalog, List<DecisionPoint> decisions,
                                    String analyzerVersion,
                                    Map<String, Map<String, String>> featuresByDecision,
                                    Instant now) {
        List<DecisionPoint> updated = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        for (DecisionPoint decision : decisions.stream().sorted(DecisionResolver.apiOrder()).toList()) {
            if (!decision.active() || decision.status() == DecisionPoint.Status.OVERRIDDEN) {
                updated.add(decision);
                matches.add(new Match(decision.id(), MatchKind.UNMATCHED, BigDecimal.ZERO, null, false));
                continue;
            }
            var signature = SemanticDecisionSignature.create(decision, analyzerVersion,
                    featuresByDecision == null ? Map.of() : featuresByDecision.getOrDefault(decision.id(), Map.of()));
            List<Candidate> candidates = catalog.entries().stream()
                    .filter(entry -> entry.signature().compatibleWith(signature))
                    .map(entry -> new Candidate(entry, entry.signature().similarity(signature)))
                    .filter(candidate -> candidate.confidence().compareTo(catalog.suggestThreshold()) >= 0)
                    .sorted(Comparator.comparing(Candidate::confidence).reversed()
                            .thenComparing(candidate -> candidate.entry().policyId())).toList();
            if (candidates.isEmpty()) {
                updated.add(decision);
                matches.add(new Match(decision.id(), MatchKind.UNMATCHED, BigDecimal.ZERO, null, false));
                continue;
            }
            BigDecimal best = candidates.get(0).confidence();
            List<Candidate> tied = candidates.stream().filter(value -> value.confidence().compareTo(best) == 0).toList();
            long choices = tied.stream().map(value -> value.entry().chosenOption()).distinct().count();
            if (choices > 1) throw new PolicyConflictException("Equal-confidence policies disagree for " + decision.id());
            DecisionPolicyEntry selected = tied.get(0).entry();
            boolean stale = !catalog.analyzerVersion().equals(analyzerVersion)
                    || !catalog.signatureSchemaVersion().equals(signature.schemaVersion())
                    || !decision.options().contains(selected.chosenOption());
            boolean auto = !stale && best.compareTo(catalog.autoConfirmThreshold()) >= 0;
            PolicyProvenance provenance = new PolicyProvenance(catalog.name(), catalog.version(), selected.policyId(),
                    best, signature.digest(), stale);
            DecisionPoint next = decision.options().contains(selected.chosenOption())
                    ? DecisionTransitions.policy(decision, selected.chosenOption(), best, provenance, auto, now)
                    : decision;
            updated.add(next);
            MatchKind kind = !decision.options().contains(selected.chosenOption()) ? MatchKind.UNMATCHED
                    : auto ? MatchKind.AUTO_CONFIRMED : MatchKind.SUGGESTED;
            matches.add(new Match(decision.id(), kind, best, selected.policyId(), stale));
        }
        long auto = matches.stream().filter(value -> value.kind() == MatchKind.AUTO_CONFIRMED).count();
        long suggested = matches.stream().filter(value -> value.kind() == MatchKind.SUGGESTED).count();
        long unmatched = matches.size() - auto - suggested;
        return new ApplyResult(updated, new ApplyReport((int) auto, (int) suggested, (int) unmatched, matches));
    }

    private record Candidate(DecisionPolicyEntry entry, BigDecimal confidence) { }
    public record ApplyResult(List<DecisionPoint> decisions, ApplyReport report) { }
    public record ApplyReport(int autoConfirmed, int suggested, int unmatched, List<Match> matches) { }
    public record Match(String decisionId, MatchKind kind, BigDecimal confidence, String policyId, boolean stale) { }
    public enum MatchKind { AUTO_CONFIRMED, SUGGESTED, UNMATCHED }
    public static final class PolicyConflictException extends IllegalArgumentException {
        public PolicyConflictException(String message) { super(message); }
    }
}
