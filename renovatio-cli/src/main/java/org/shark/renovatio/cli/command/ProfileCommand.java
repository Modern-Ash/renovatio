package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.OutputWriter;
import org.shark.renovatio.cli.ReusableProjectStore;
import org.shark.renovatio.profile.FileProfileTemplateRepository;
import org.shark.renovatio.profile.ProfileTemplates;
import org.shark.renovatio.profile.TemplateReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;

@Command(name = "profile", description = "Manage reusable migration profile templates",
        subcommands = {ProfileCommand.Save.class, ProfileCommand.Apply.class,
                ProfileCommand.Diff.class, ProfileCommand.ListTemplates.class})
public final class ProfileCommand implements Runnable {
    @Override public void run() { }

    private abstract static class Base implements Callable<Integer> {
        @Option(names = "--json") boolean json;
        FileProfileTemplateRepository repository() {
            return new FileProfileTemplateRepository(ReusableProjectStore.assetsRoot().resolve("profiles"));
        }
        OutputWriter output() { return new OutputWriter(json); }
    }

    @Command(name = "save", description = "Save a project's profile as an immutable template version")
    static final class Save extends Base {
        @Parameters(index = "0") String name;
        @Option(names = "--version", required = true) String version;
        @Option(names = "--project", required = true) Path project;
        @Option(names = "--description") String description;
        @Override public Integer call() {
            var template = ProfileTemplates.snapshot(name, version, description,
                    new ReusableProjectStore(project).profile(), Instant.now());
            template = repository().save(template);
            if (json) output().writeJson(template); else output().line("Saved profile template " + name + "@" + version);
            return 0;
        }
    }

    @Command(name = "apply", description = "Bind an explicit template version to a project")
    static final class Apply extends Base {
        @Parameters(index = "0") String name;
        @Option(names = "--version", required = true) String version;
        @Option(names = "--project", required = true) Path project;
        @Override public Integer call() {
            var reference = new TemplateReference(name, version);
            var template = repository().find(reference).orElseThrow(() -> new IllegalArgumentException("Unknown profile template " + name + "@" + version));
            var store = new ReusableProjectStore(project);
            store.templateBinding(reference);
            var effective = ProfileTemplates.effective(template, store.profile());
            if (json) output().writeJson(effective); else output().line("Applied profile template " + name + "@" + version);
            return 0;
        }
    }

    @Command(name = "diff", description = "Show project deviations from an explicit template version")
    static final class Diff extends Base {
        @Parameters(index = "0") Path project;
        @Parameters(index = "1") String name;
        @Option(names = "--version", required = true) String version;
        @Override public Integer call() {
            var template = repository().find(new TemplateReference(name, version))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown profile template " + name + "@" + version));
            var diff = ProfileTemplates.diff(template, new ReusableProjectStore(project).profile());
            if (json) output().writeJson(diff); else {
                output().line(diff.size() + " profile deviation(s)");
                diff.forEach(value -> output().line(value.changeKind() + " " + value.path()));
            }
            return 0;
        }
    }

    @Command(name = "list", description = "List reusable profile versions")
    static final class ListTemplates extends Base {
        @Override public Integer call() {
            var values = repository().list();
            if (json) output().writeJson(values); else values.forEach(value -> output().line(value.name() + "@" + value.version()));
            return 0;
        }
    }
}
