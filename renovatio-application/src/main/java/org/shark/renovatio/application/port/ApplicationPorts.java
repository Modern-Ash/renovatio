package org.shark.renovatio.application.port;

import org.shark.renovatio.application.model.ApplicationModel.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** All external effects and capabilities used by the application pipeline. */
public final class ApplicationPorts {
    private ApplicationPorts() { }

    public interface SourceAnalyzer {
        SourceSnapshot snapshot(String projectId);
        Analysis analyze(SourceSnapshot snapshot);
    }
    public interface ProposalProvider { DomainReview review(String projectId, Analysis analysis); }
    public interface ArchitectureProjector { Object project(Analysis analysis, Map<String, String> decisions); }
    public interface TargetEmitter { Map<String, byte[]> emit(Object projection); }
    public interface TargetRefiner { Map<String, byte[]> refine(Map<String, byte[]> artifacts); }
    public interface ValidationGate { ValidationResult validate(ArtifactManifest manifest); }
    public interface ProjectRepository {
        Optional<Project> find(String projectId);
        void save(Project project);
        Optional<Analysis> analysis(String projectId);
        void saveAnalysis(String projectId, Analysis analysis);
        Optional<DomainReview> review(String projectId);
        void saveReview(String projectId, DomainReview review);
        Map<String, String> decisions(String projectId);
        void saveDecisions(String projectId, Map<String, String> decisions);
        Optional<MigrationPlan> plan(String projectId, String planId);
        void savePlan(MigrationPlan plan);
        Optional<ArtifactManifest> manifest(String projectId, String manifestId);
        void saveManifest(ArtifactManifest manifest);
    }
    public interface ArtifactRepository {
        Map<String, byte[]> read(String projectId);
        /** Atomically replaces the complete artifact set or leaves the previous set untouched. */
        void replace(String projectId, Map<String, byte[]> artifacts);
        void saveChangeSet(ChangeSet changeSet);
        List<ChangeSet> history(String projectId);
    }
    public interface IdempotencyRepository {
        Optional<IdempotencyRecord> find(String projectId, String operation, String key);
        void save(IdempotencyRecord record);
    }
    public interface GitPort {
        /** Creates a local, compensable checkpoint; remote pushes are expressly outside this port. */
        String checkpoint(String projectId, String changeSetId);
        void compensate(String projectId, String checkpoint);
    }
    public interface NetworkPort { void publish(String projectId, Map<String, byte[]> payload); }
    public interface ClockPort { Instant now(); }
}
