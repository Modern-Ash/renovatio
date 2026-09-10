package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.OutputWriter;
import org.shark.renovatio.cli.ReusableProjectStore;
import org.shark.renovatio.decisions.DecisionPolicies;
import org.shark.renovatio.decisions.FileDecisionPolicyRepository;
import org.shark.renovatio.decisions.PolicyReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "policy", description = "Manage reusable decision-policy catalogs",
        subcommands = {PolicyCommand.Export.class, PolicyCommand.Apply.class, PolicyCommand.ListPolicies.class})
public final class PolicyCommand implements Runnable {
    private static final String ANALYZER_VERSION = "renovatio-f8-v1";
    @Override public void run() { }

    private abstract static class Base implements Callable<Integer> {
        @Option(names = "--json") boolean json;
        FileDecisionPolicyRepository repository() {
            return new FileDecisionPolicyRepository(ReusableProjectStore.assetsRoot().resolve("policies"));
        }
        OutputWriter output() { return new OutputWriter(json); }
    }

    @Command(name = "export", description = "Export confirmed project decisions as an immutable catalog")
    static final class Export extends Base {
        @Parameters(index = "0") String name;
        @Option(names = "--version", required = true) String version;
        @Option(names = "--project", required = true) Path project;
        @Override public Integer call() {
            var store = new ReusableProjectStore(project);
            var catalog = DecisionPolicies.exportCatalog(name, version, project.toAbsolutePath().normalize().toString(),
                    ANALYZER_VERSION, store.decisions(), Map.of(), null, null, Instant.now());
            catalog = repository().save(catalog);
            if (json) output().writeJson(catalog); else output().line("Exported policy catalog " + name + "@" + version
                    + " with " + catalog.entries().size() + " policies");
            return 0;
        }
    }

    @Command(name = "apply", description = "Bind and apply an explicit policy-catalog version")
    static final class Apply extends Base {
        @Parameters(index = "0") String name;
        @Option(names = "--version", required = true) String version;
        @Option(names = "--project", required = true) Path project;
        @Override public Integer call() {
            var reference = new PolicyReference(name, version);
            var catalog = repository().find(reference).orElseThrow(() -> new IllegalArgumentException("Unknown policy catalog " + name + "@" + version));
            var store = new ReusableProjectStore(project);
            var result = DecisionPolicies.apply(catalog, store.decisions(), ANALYZER_VERSION, Map.of(), Instant.now());
            store.decisions(result.decisions());
            store.policyBinding(reference);
            if (json) output().writeJson(result.report()); else output().line(result.report().autoConfirmed()
                    + " auto-confirmed · " + result.report().suggested() + " suggested · "
                    + result.report().unmatched() + " unmatched");
            return 0;
        }
    }

    @Command(name = "list", description = "List reusable policy-catalog versions")
    static final class ListPolicies extends Base {
        @Override public Integer call() {
            var values = repository().list();
            if (json) output().writeJson(values); else values.forEach(value -> output().line(value.name() + "@" + value.version()));
            return 0;
        }
    }
}
