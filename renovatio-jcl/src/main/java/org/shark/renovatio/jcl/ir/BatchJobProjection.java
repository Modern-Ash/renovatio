package org.shark.renovatio.jcl.ir;

import org.shark.renovatio.jcl.classify.StepClassifier;
import org.shark.renovatio.jcl.parse.CondClause;
import org.shark.renovatio.jcl.parse.DdStatement;
import org.shark.renovatio.jcl.parse.JclJob;
import org.shark.renovatio.jcl.parse.JclStep;
import org.shark.renovatio.semantic.ir.BatchDataset;
import org.shark.renovatio.semantic.ir.BatchJob;
import org.shark.renovatio.semantic.ir.BatchStep;
import org.shark.renovatio.semantic.ir.ConditionGraph;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Projects a parsed JCL job into target-neutral semantic IR. */
public final class BatchJobProjection {
    private static final Pattern IF_RETURN_CODE_REFERENCE = Pattern.compile(
            "(?i)\\b([A-Z0-9_$#@-]+)\\.RC\\b");
    private final StepClassifier classifier;

    public BatchJobProjection() { this(new StepClassifier()); }
    public BatchJobProjection(StepClassifier classifier) { this.classifier = java.util.Objects.requireNonNull(classifier); }

    public BatchJob project(JclJob job, List<SemanticProgram> migratedPrograms) {
        return project(job, migratedPrograms, job.sourceSha256());
    }

    public BatchJob project(JclJob job, List<SemanticProgram> migratedPrograms, String sourceHash) {
        Set<String> migrated = (migratedPrograms == null ? List.<SemanticProgram>of() : migratedPrograms).stream()
                .map(SemanticProgram::programId).map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        List<BatchDataset> datasets = new ArrayList<>();
        List<BatchStep> steps = new ArrayList<>();
        Map<String, String> stepIds = new LinkedHashMap<>();
        for (int ordinal = 0; ordinal < job.steps().size(); ordinal++) {
            JclStep source = job.steps().get(ordinal);
            String stepId = BatchJob.stepId(job.jobName(), source.stepName(), ordinal);
            stepIds.put(source.stepName(), stepId);
            List<String> datasetRefs = new ArrayList<>();
            for (DdStatement dd : source.ddStatements()) {
                String id = BatchJob.datasetId(job.jobName(), source.stepName(), dd.ddName());
                datasetRefs.add(id);
                datasets.add(new BatchDataset(id, dd.ddName(), access(dd),
                        dd.dsn(), dd.instreamData(), dd.concatenations().stream()
                        .map(part -> new BatchDataset.ResourcePart(part.dsn(), part.instreamData())).toList()));
            }
            StepClassifier.Classification result = classifier.classify(source, migrated);
            steps.add(new BatchStep(stepId, source.stepName(), ordinal, result.kind(), result.programRef(),
                    result.utility(), datasetRefs, result.residueReason()));
        }

        List<ConditionGraph.Guard> guards = new ArrayList<>();
        for (int ordinal = 0; ordinal < job.steps().size(); ordinal++) {
            JclStep source = job.steps().get(ordinal);
            String member = stepIds.get(source.stepName());
            if (source.condition().isPresent()) {
                CondClause condition = source.condition().get();
                if (condition.kind() == CondClause.Kind.NORMAL) {
                    for (CondClause.Predicate predicate : condition.predicates()) {
                        if (predicate.referencedStep().isPresent()) {
                            String referencedName = predicate.referencedStep().orElseThrow();
                            String referencedId = stepIds.get(referencedName);
                            if (referencedId == null)
                                throw new IllegalArgumentException("COND references unknown step " + referencedName);
                            guards.add(new ConditionGraph.Guard(predicate.normalized(), Optional.of(referencedId),
                                    predicate.truthTable(), List.of(member)));
                        } else {
                            for (int previous = 0; previous < ordinal; previous++) {
                                String referencedId = stepIds.get(job.steps().get(previous).stepName());
                                guards.add(new ConditionGraph.Guard(predicate.normalized(), Optional.of(referencedId),
                                        predicate.truthTable(), List.of(member)));
                            }
                        }
                    }
                } else {
                    Optional<String> referenced = ordinal == 0 ? Optional.empty()
                            : Optional.of(stepIds.get(job.steps().get(ordinal - 1).stepName()));
                    guards.add(new ConditionGraph.Guard(condition.normalizedExpression(), referenced,
                            condition.truthTable(), List.of(member)));
                }
            }
            if (source.ifExpression().isPresent()) {
                LinkedHashMap<String, Boolean> table = new LinkedHashMap<>();
                table.put("PREDICATE=FALSE", false);
                table.put("PREDICATE=TRUE", true);
                List<String> referencedIds = ifReferencedStepIds(source.ifExpression().get(), stepIds);
                if (referencedIds.isEmpty())
                    guards.add(new ConditionGraph.Guard(source.ifExpression().get(), Optional.empty(), table, List.of(member)));
                else referencedIds.forEach(referencedId -> guards.add(new ConditionGraph.Guard(
                        source.ifExpression().get(), Optional.of(referencedId), table, List.of(member))));
            }
        }
        SourceProvenance provenance = new SourceProvenance(job.sourcePath(), sourceHash, "JCL",
                Optional.of("IBM"), List.of());
        return new BatchJob(BatchJob.SCHEMA_VERSION, job.jobName(), provenance, steps, datasets,
                new ConditionGraph(guards));
    }

    private static BatchDataset.AccessKind access(DdStatement dd) {
        String name = dd.ddName().toUpperCase(Locale.ROOT);
        String dsn = dd.dsn().orElse("").toUpperCase(Locale.ROOT);
        if (dd.temporary()) return BatchDataset.AccessKind.TEMP;
        if (dd.sysout() || name.equals("SYSIN") || name.equals("SYSOUT")) return BatchDataset.AccessKind.STDIO;
        if (dd.parameters().containsKey("AMP") || dsn.contains("VSAM") || dsn.endsWith(".KSDS"))
            return BatchDataset.AccessKind.VSAM;
        if (dd.disposition().contains("NEW") || dd.disposition().contains("MOD")
                || name.endsWith("OUT") || name.equals("SYSUT2")) return BatchDataset.AccessKind.SEQ_OUT;
        return BatchDataset.AccessKind.SEQ_IN;
    }

    private static List<String> ifReferencedStepIds(String expression, Map<String, String> stepIds) {
        Matcher matcher = IF_RETURN_CODE_REFERENCE.matcher(expression);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        while (matcher.find()) {
            String name = matcher.group(1).toUpperCase(Locale.ROOT);
            String id = stepIds.get(name);
            if (id == null) throw new IllegalArgumentException("IF references unknown step " + name);
            result.add(id);
        }
        return List.copyOf(result);
    }

}
