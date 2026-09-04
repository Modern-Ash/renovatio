package org.shark.renovatio.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.PolicyReference;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.profile.TemplateReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

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
}
