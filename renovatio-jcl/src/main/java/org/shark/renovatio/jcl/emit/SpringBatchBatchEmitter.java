package org.shark.renovatio.jcl.emit;

import org.shark.renovatio.profile.BatchTargets;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.BatchDataset;
import org.shark.renovatio.semantic.ir.BatchJob;
import org.shark.renovatio.semantic.ir.BatchStep;
import org.shark.renovatio.semantic.ir.ConditionGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Emits deterministic Spring Batch Java configuration without depending on Spring at generator runtime. */
public final class SpringBatchBatchEmitter implements BatchEmitter {
    @Override public boolean supports(MigrationProfile.BatchTarget target) {
        return target == MigrationProfile.BatchTarget.SPRING_BATCH;
    }

    @Override public BatchEmission emit(BatchJob job, MigrationProfile profile) {
        Objects.requireNonNull(job, "job");
        if (!supports(BatchTargets.resolve(profile)))
            throw new IllegalArgumentException("Only SPRING_BATCH is active in F7");
        String className = javaName(job.jobId()) + "BatchConfiguration";
        List<String> actions = job.steps().stream().filter(step -> step.kind() == BatchStep.Kind.RESIDUE)
                .map(step -> step.residueReason().orElseThrow()).toList();
        return new BatchEmission(Map.of("src/main/java/generated/batch/" + className + ".java",
                source(job, className)), actions);
    }

    private static String source(BatchJob job, String className) {
        StringBuilder out = new StringBuilder();
        out.append("package generated.batch;\n\n")
                .append("import org.springframework.batch.core.Job;\n")
                .append("import org.springframework.batch.core.Step;\n")
                .append("import org.springframework.batch.core.job.builder.JobBuilder;\n")
                .append("import org.springframework.batch.core.step.builder.StepBuilder;\n")
                .append("import org.springframework.batch.core.repository.JobRepository;\n")
                .append("import org.springframework.context.annotation.Bean;\n")
                .append("import org.springframework.context.annotation.Configuration;\n")
                .append("import org.springframework.transaction.PlatformTransactionManager;\n\n")
                .append("@Configuration\npublic class ").append(className).append(" {\n")
                .append("  private final JobRepository jobs;\n")
                .append("  private final PlatformTransactionManager transactions;\n")
                .append("  private final MigratedProgramInvoker programs;\n")
                .append("  private final BatchRuntime runtime;\n\n")
                .append("  public ").append(className)
                .append("(JobRepository jobs, PlatformTransactionManager transactions, MigratedProgramInvoker programs, BatchRuntime runtime) {\n")
                .append("    this.jobs = jobs; this.transactions = transactions; this.programs = programs; this.runtime = runtime;\n  }\n\n");

        for (BatchStep step : job.steps()) emitStep(out, step, guards(job, step), datasets(job, step));
        out.append("  @Bean\n  public Job ").append(javaIdentifier(job.jobId())).append("Job() {\n")
                .append("    return new JobBuilder(\"").append(escape(job.jobId())).append("\", jobs)");
        if (job.steps().isEmpty()) out.append(".preventRestart()") ;
        else {
            out.append(".start(").append(method(job.steps().get(0))).append("())");
            for (int index = 1; index < job.steps().size(); index++)
                out.append(".next(").append(method(job.steps().get(index))).append("())");
        }
        out.append(".build();\n  }\n\n")
                .append("  public interface MigratedProgramInvoker { void run(String program, java.util.Map<String,String> resources) throws Exception; }\n")
                .append("  public interface BatchRuntime {\n")
                .append("    boolean shouldRun(String predicate, org.springframework.batch.core.scope.context.ChunkContext context);\n")
                .append("    void utility(String utility, java.util.Map<String,String> resources) throws Exception;\n  }\n")
                .append("  public static final class UnsupportedResidueException extends RuntimeException {\n")
                .append("    public UnsupportedResidueException(String message) { super(message); }\n  }\n")
                .append("}\n");
        return out.toString();
    }

    private static void emitStep(StringBuilder out, BatchStep step, List<ConditionGraph.Guard> guards,
                                 List<BatchDataset> datasets) {
        out.append("  @Bean\n  public Step ").append(method(step)).append("() {\n")
                .append("    return new StepBuilder(\"").append(escape(step.stepName())).append("\", jobs).tasklet((contribution, context) -> {\n");
        for (ConditionGraph.Guard guard : guards)
            out.append("      if (!runtime.shouldRun(\"").append(escape(guard.predicate())).append("\", context)) return org.springframework.batch.repeat.RepeatStatus.FINISHED;\n");
        String resources = resourceMap(datasets);
        switch (step.kind()) {
            case MIGRATED_PROGRAM_CALL -> out.append("      programs.run(\"").append(escape(step.programRef().orElseThrow()))
                    .append("\", ").append(resources).append(");\n");
            case STANDARD_UTILITY -> out.append("      runtime.utility(\"").append(escape(step.utility().orElseThrow()))
                    .append("\", ").append(resources).append(");\n");
            case RESIDUE -> out.append("      throw new UnsupportedResidueException(\"")
                    .append(escape(step.residueReason().orElseThrow())).append("\");\n");
        }
        if (step.kind() != BatchStep.Kind.RESIDUE)
            out.append("      return org.springframework.batch.repeat.RepeatStatus.FINISHED;\n");
        out.append("    }, transactions).build();\n  }\n\n");
    }

    private static List<ConditionGraph.Guard> guards(BatchJob job, BatchStep step) {
        return job.conditionGraph().guards().stream().filter(guard -> guard.memberStepIds().contains(step.id())).toList();
    }

    private static List<BatchDataset> datasets(BatchJob job, BatchStep step) {
        return job.datasets().stream().filter(value -> step.datasetRefs().contains(value.id())).toList();
    }

    private static String resourceMap(List<BatchDataset> datasets) {
        if (datasets.isEmpty()) return "java.util.Map.of()";
        List<String> entries = new ArrayList<>();
        for (BatchDataset dataset : datasets) {
            String resource = dataset.access() == BatchDataset.AccessKind.TEMP
                    ? "memory:" + dataset.id() : dataset.resourceReference().orElse(dataset.ddName());
            entries.add("java.util.Map.entry(\"" + escape(dataset.ddName()) + "\", \"" + escape(resource) + "\")");
        }
        return "java.util.Map.ofEntries(" + String.join(", ", entries) + ")";
    }

    private static String method(BatchStep step) { return javaIdentifier(step.stepName()) + "Step"; }
    private static String javaName(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
            if (!part.isBlank()) result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        return result.isEmpty() ? "Batch" : result.toString();
    }
    private static String javaIdentifier(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return Character.isJavaIdentifierStart(result.charAt(0)) ? result : "_" + result;
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
