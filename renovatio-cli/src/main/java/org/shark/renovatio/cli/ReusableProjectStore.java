package org.shark.renovatio.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.decisions.DecisionTransitions;
import org.shark.renovatio.decisions.F1DecisionCatalog;
import org.shark.renovatio.decisions.PolicyReference;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.profile.TemplateReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Small local-project adapter used by reusable profile/policy CLI commands. */
public final class ReusableProjectStore {
    private final Path state;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public ReusableProjectStore(Path project) {
        this.state = project.toAbsolutePath().normalize().resolve(".renovatio");
    }

    public MigrationProfile profile() {
        Path file = state.resolve("migration-profile.json");
        if (!Files.isRegularFile(file)) return MigrationProfiles.emptyOverlay();
        try { return MigrationProfiles.readJson(Files.readString(file)); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    public void profile(MigrationProfile profile) { write(state.resolve("migration-profile.json"), profile); }

    public List<DecisionPoint> decisions() {
        Path file = state.resolve("decisions.json");
        if (!Files.isRegularFile(file)) return List.of();
        try { return json.readValue(file.toFile(), new TypeReference<List<DecisionPoint>>() { }); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    public void decisions(List<DecisionPoint> decisions) { write(state.resolve("decisions.json"), decisions); }
    public void templateBinding(TemplateReference reference) { write(state.resolve("profile-template.json"), reference); }
    public void policyBinding(PolicyReference reference) { write(state.resolve("policy-catalog.json"), reference); }

    public Optional<TemplateReference> templateBinding() {
        return read(state.resolve("profile-template.json"), TemplateReference.class);
    }

    public Optional<PolicyReference> policyBinding() {
        return read(state.resolve("policy-catalog.json"), PolicyReference.class);
    }

    /** Resolves the local CLI project using the same F8 precedence and hash envelope as the API. */
    public MigrationProfiles.EffectiveProfile effectiveProfile() {
        List<DecisionPoint> all = decisions();
        List<DecisionPoint> inherited = all.stream()
                .filter(value -> value.source() == DecisionPoint.Source.POLICY).toList();
        List<DecisionPoint> local = all.stream()
                .filter(value -> value.source() != DecisionPoint.Source.POLICY).toList();
        return new DecisionResolver().resolve(MigrationProfiles.emptyOverlay(), templateBinding().orElse(null),
                inherited, policyBinding().orElse(null), profile(), local);
    }

    /** Reconciles a successful CLI analysis into durable F1 decision state. */
    public List<DecisionPoint> reconcileAnalysis(String semanticIrHash, Instant now) {
        List<DecisionPoint> generated = F1DecisionCatalog.create(semanticIrHash, now);
        List<DecisionPoint> current = decisions();
        List<DecisionPoint> next = new ArrayList<>();
        for (DecisionPoint heuristic : generated) {
            DecisionPoint existing = current.stream()
                    .filter(value -> value.id().equals(heuristic.id())).findFirst().orElse(null);
            next.add(existing == null ? heuristic : DecisionTransitions.reconcile(existing, heuristic, now));
        }
        current.stream().filter(value -> generated.stream().noneMatch(item -> item.id().equals(value.id())))
                .map(value -> DecisionTransitions.retire(value, now)).forEach(next::add);
        List<DecisionPoint> ordered = next.stream().sorted(DecisionResolver.apiOrder()).toList();
        decisions(ordered);
        return ordered;
    }

    public static Path assetsRoot() {
        String configured = System.getProperty("renovatio.assets.root");
        if (configured != null && !configured.isBlank()) return Path.of(configured);
        return Path.of(System.getProperty("user.home"), ".renovatio");
    }

    private void write(Path target, Object value) {
        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), ".renovatio-", ".tmp");
            try {
                json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
                try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException exception) { throw new UncheckedIOException("Cannot write reusable project state", exception); }
    }

    private <T> Optional<T> read(Path file, Class<T> type) {
        if (!Files.isRegularFile(file)) return Optional.empty();
        try { return Optional.of(json.readValue(file.toFile(), type)); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }
}
