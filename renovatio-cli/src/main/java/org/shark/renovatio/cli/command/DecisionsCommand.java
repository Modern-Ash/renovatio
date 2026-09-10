package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.OutputWriter;
import org.shark.renovatio.cli.ReusableProjectStore;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.decisions.DecisionTransitions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Callable;

/** Reviews and updates the durable F1 decisions produced by CLI analysis. */
@Command(name = "decisions", description = "Review or set migration decisions",
        subcommands = {DecisionsCommand.ListDecisions.class, DecisionsCommand.SetDecision.class})
public final class DecisionsCommand implements Runnable {
    @Override public void run() { }

    private abstract static class Base implements Callable<Integer> {
        @Option(names = "--project", required = true) Path project;
        @Option(names = "--json") boolean json;
        ReusableProjectStore store() { return new ReusableProjectStore(project); }
        OutputWriter output() { return new OutputWriter(json); }
    }

    @Command(name = "list", description = "List decisions created by project analysis")
    static final class ListDecisions extends Base {
        @Override public Integer call() {
            var decisions = store().decisions().stream().sorted(DecisionResolver.apiOrder()).toList();
            if (json) output().writeJson(decisions);
            else decisions.forEach(value -> output().line(value.decisionKey() + " = " + value.chosenOption()
                    + " [" + value.status() + ", " + value.source() + "]"));
            return 0;
        }
    }

    @Command(name = "set", description = "Confirm or override one analyzed decision")
    static final class SetDecision extends Base {
        @Parameters(index = "0", paramLabel = "<decision>",
                description = "Decision id or decision key shown by 'decisions list'.")
        String decision;
        @Parameters(index = "1", paramLabel = "<option>", description = "One of the decision's allowed options.")
        String option;

        @Override public Integer call() {
            var current = store().decisions();
            DecisionPoint selected = current.stream().filter(DecisionPoint::active)
                    .filter(value -> value.id().equals(decision) || value.decisionKey().equals(decision))
                    .findFirst().orElse(null);
            if (selected == null) {
                System.err.println("error: unknown decision " + decision + " — run 'renovatio decisions list' first");
                return 1;
            }
            if (!selected.options().contains(option)) {
                System.err.println("error: invalid option " + option + " — allowed: "
                        + String.join(", ", selected.options()));
                return 1;
            }
            DecisionPoint updated = DecisionTransitions.patch(selected, option, selected.revision(), Instant.now());
            var next = new ArrayList<>(current);
            next.replaceAll(value -> value.id().equals(updated.id()) ? updated : value);
            store().decisions(next.stream().sorted(DecisionResolver.apiOrder()).toList());
            if (json) output().writeJson(updated);
            else output().line("Set " + updated.decisionKey() + " = " + updated.chosenOption()
                    + " [" + updated.status() + "]");
            return 0;
        }
    }
}
