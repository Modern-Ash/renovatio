package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Pure resolver for the accepted deterministic module-grouping precedence. */
public final class ModuleGroupingResolver {
    public GroupingResult resolve(ArchitectureRequest request) {
        Objects.requireNonNull(request, "request");
        MigrationProfile.ModuleGrouping mode = request.effectiveProfile().profile().architecture().moduleGrouping();
        return switch (mode) {
            case BY_PROGRAM -> byProgram(request);
            case BY_DOMAIN -> byDomain(request);
            case SINGLE_MODULE -> singleModule(request);
        };
    }

    private GroupingResult byProgram(ArchitectureRequest request) {
        Map<String, String> assignments = new TreeMap<>();
        request.programs().forEach(program -> assignments.put(program.programId(),
                request.grouping().manualModules().getOrDefault(program.programId(),
                        ArchitectureSupport.moduleName(program.programId()))));
        return result(assignments, request, Set.of());
    }

    private GroupingResult byDomain(ArchitectureRequest request) {
        Map<String, String> assignments = new TreeMap<>();
        Set<String> usedRules = new LinkedHashSet<>();
        for (SemanticProgram program : request.programs()) {
            String manual = request.grouping().manualModules().get(program.programId());
            if (manual != null) {
                assignments.put(program.programId(), manual);
                usedRules.add("manual:" + program.programId());
                continue;
            }
            Set<String> copybookModules = new LinkedHashSet<>();
            for (String copybook : request.programCopybooks().getOrDefault(program.programId(), List.of())) {
                String module = request.grouping().domainCopybooks().get(copybook);
                if (module != null) {
                    copybookModules.add(module);
                    usedRules.add("copybook:" + copybook);
                }
            }
            if (copybookModules.size() > 1) {
                throw new IllegalArgumentException("conflicting domain copybook modules for " + program.programId());
            }
            if (copybookModules.size() == 1) {
                assignments.put(program.programId(), copybookModules.iterator().next());
                continue;
            }
            String prefixModule = longestPrefix(program.programId(), request.grouping().prefixModules(), usedRules);
            assignments.put(program.programId(), prefixModule == null
                    ? ArchitectureSupport.moduleName(program.programId()) : prefixModule);
        }
        return result(assignments, request, usedRules);
    }

    private GroupingResult singleModule(ArchitectureRequest request) {
        Set<String> manual = new LinkedHashSet<>(request.grouping().manualModules().values());
        if (manual.size() > 1 || (!manual.isEmpty() && !manual.contains(request.grouping().singleModuleName()))) {
            throw new IllegalArgumentException("manual assignments contradict SINGLE_MODULE");
        }
        Map<String, String> assignments = new TreeMap<>();
        request.programs().forEach(program -> assignments.put(program.programId(),
                request.grouping().singleModuleName()));
        return result(assignments, request, request.grouping().manualModules().isEmpty()
                ? Set.of() : request.grouping().manualModules().keySet().stream()
                        .map(value -> "manual:" + value).collect(java.util.stream.Collectors.toSet()));
    }

    private static String longestPrefix(String programId, Map<String, String> rules, Set<String> usedRules) {
        int length = -1;
        String selected = null;
        for (Map.Entry<String, String> entry : rules.entrySet()) {
            if (!programId.startsWith(entry.getKey())) continue;
            if (entry.getKey().length() > length) {
                length = entry.getKey().length();
                selected = entry.getValue();
            } else if (entry.getKey().length() == length && !entry.getValue().equals(selected)) {
                throw new IllegalArgumentException("conflicting equal-length prefix modules for " + programId);
            }
        }
        if (length >= 0) {
            int selectedLength = length;
            String selectedModule = selected;
            rules.forEach((prefix, module) -> {
                if (programId.startsWith(prefix) && prefix.length() == selectedLength && module.equals(selectedModule)) {
                    usedRules.add("prefix:" + prefix);
                }
            });
        }
        return selected;
    }

    private static GroupingResult result(Map<String, String> assignments, ArchitectureRequest request,
                                         Set<String> usedRules) {
        List<String> unused = new ArrayList<>();
        request.grouping().manualModules().keySet().forEach(value -> addUnused(unused, usedRules, "manual:" + value));
        request.grouping().domainCopybooks().keySet().forEach(value -> addUnused(unused, usedRules, "copybook:" + value));
        request.grouping().prefixModules().keySet().forEach(value -> addUnused(unused, usedRules, "prefix:" + value));
        return new GroupingResult(assignments, unused);
    }

    private static void addUnused(List<String> target, Set<String> used, String rule) {
        if (!used.contains(rule)) target.add(rule);
    }

    public record GroupingResult(Map<String, String> moduleByProgram, List<String> unusedRules) {
        public GroupingResult {
            TreeMap<String, String> ordered = new TreeMap<>();
            Objects.requireNonNull(moduleByProgram, "moduleByProgram").forEach((key, value) ->
                    ordered.put(ArchitectureSupport.program(key), ArchitectureSupport.moduleName(value)));
            moduleByProgram = Collections.unmodifiableMap(new LinkedHashMap<>(ordered));
            unusedRules = (unusedRules == null ? List.<String>of() : unusedRules).stream().distinct().sorted().toList();
        }
    }
}
