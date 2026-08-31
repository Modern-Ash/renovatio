package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.RenovatioCliContext;
import org.shark.renovatio.core.service.MigrationReportService;
import org.shark.renovatio.shared.domain.MigrationReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "report", description = "Render the migration report as HTML or PDF.")
public final class ReportCommand implements java.util.concurrent.Callable<Integer> {

    @Option(names = "--html", description = "Output file path for HTML report.")
    String htmlPath;

    @Option(names = "--pdf", description = "Output file path for PDF report.")
    String pdfPath;

    @Override
    public Integer call() {
        boolean hasHtml = htmlPath != null && !htmlPath.isBlank();
        boolean hasPdf = pdfPath != null && !pdfPath.isBlank();

        if (!hasHtml && !hasPdf) {
            System.err.println("error: specify exactly one of --html or --pdf");
            return 2;
        }
        if (hasHtml && hasPdf) {
            System.err.println("error: --html and --pdf are mutually exclusive");
            return 2;
        }

        try {
            MigrationReportService reportService = RenovatioCliContext.shared()
                    .bean(MigrationReportService.class);
            MigrationReport report = reportService.aggregateReport();

            if (hasHtml) {
                String html = reportService.renderHtml(report);
                Path out = Path.of(htmlPath);
                Files.createDirectories(out.getParent());
                Files.writeString(out, html);
                System.out.println("report written to " + out.toAbsolutePath());
            } else {
                byte[] pdf = reportService.renderPdf(report);
                Path out = Path.of(pdfPath);
                Files.createDirectories(out.getParent());
                Files.write(out, pdf);
                System.out.println("report written to " + out.toAbsolutePath());
            }
            return 0;
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            return 1;
        }
    }
}
