package org.shark.renovatio.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Minimal root command for testing individual subcommands without booting Spring context.
 */
@Command(name = "renovatio", subcommands = {
        AnalyzeCommand.class,
        MetricsCommand.class,
        PlanCommand.class,
        ApplyCommand.class,
        DiffCommand.class,
        ReviewCommand.class,
        ReportCommand.class,
        ServeCommand.class
})
class RenovatioCliStub implements Runnable {
    @Override
    public void run() {
        // no-op
    }
}
