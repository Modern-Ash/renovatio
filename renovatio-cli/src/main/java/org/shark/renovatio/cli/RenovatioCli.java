package org.shark.renovatio.cli;

import org.shark.renovatio.cli.command.AnalyzeCommand;
import org.shark.renovatio.cli.command.ApplyCommand;
import org.shark.renovatio.cli.command.DecisionsCommand;
import org.shark.renovatio.cli.command.DiffCommand;
import org.shark.renovatio.cli.command.GenerateCommand;
import org.shark.renovatio.cli.command.MetricsCommand;
import org.shark.renovatio.cli.command.PlanCommand;
import org.shark.renovatio.cli.command.ReportCommand;
import org.shark.renovatio.cli.command.ReviewCommand;
import org.shark.renovatio.cli.command.ServeCommand;
import org.shark.renovatio.cli.command.ProfileCommand;
import org.shark.renovatio.cli.command.PolicyCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Root command for the Renovatio CLI.
 *
 * <p>Exposes the COBOL migration capabilities as ordinary subcommands that boot the Spring
 * application context headless and call {@code LanguageProviderRegistry} in-process.
 */
@Command(
        name = "renovatio",
        mixinStandardHelpOptions = true,
        version = "renovatio-cli 0.0.1-SNAPSHOT",
        description = "Command-line adapter over the in-process Renovatio core",
        subcommands = {
                AnalyzeCommand.class,
                MetricsCommand.class,
                GenerateCommand.class,
                PlanCommand.class,
                ApplyCommand.class,
                DiffCommand.class,
                ReviewCommand.class,
                ReportCommand.class,
                ProfileCommand.class,
                DecisionsCommand.class,
                PolicyCommand.class,
                ServeCommand.class
        }
)
public final class RenovatioCli implements Runnable {

    public static void main(String[] args) {
        System.exit(new CommandLine(new RenovatioCli()).execute(args));
    }

    @Override
    public void run() {
        // Picocli prints usage when users ask for help.
    }
}
