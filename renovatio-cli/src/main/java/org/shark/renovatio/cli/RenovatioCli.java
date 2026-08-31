package org.shark.renovatio.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Root command for the Renovatio CLI.
 *
 * <p>The phase-1 module wires the bootstrap and help surface now, while the concrete subcommands
 * land in later tasks.
 */
@Command(
        name = "renovatio",
        mixinStandardHelpOptions = true,
        version = "renovatio-cli 0.0.1-SNAPSHOT",
        description = "Command-line adapter over the in-process Renovatio core"
)
public final class RenovatioCli implements Runnable {

    public static void main(String[] args) {
        System.exit(new CommandLine(new RenovatioCli()).execute(args));
    }

    @Override
    public void run() {
        // Picocli prints usage when users ask for help. The root command itself is a no-op until
        // the phase-1 subcommands are wired in.
    }
}
