package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.core.service.MigrationReportService;
import org.shark.renovatio.shared.domain.MigrationReport;
import org.springframework.http.HttpHeaders;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/report")
public class ProjectReportController {
    private final MigrationReportService reportService;
    private final ApiAccessService accessService;

    public ProjectReportController(MigrationReportService reportService, ApiAccessService accessService) {
        this.reportService = reportService;
        this.accessService = accessService;
    }

    @GetMapping("/{format}")
    public ResponseEntity<?> getReport(
            @PathVariable String projectId,
            @PathVariable String format,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        MigrationReport report = reportService.aggregateReport();
        String normalizedFormat = format.toLowerCase();
        return switch (normalizedFormat) {
            case "html" -> ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(reportService.renderHtml(report));
            case "json" -> ResponseEntity.ok(report);
            case "pdf" -> {
                byte[] pdf = reportService.renderPdf(report);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + projectId + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdf);
            }
            default -> ResponseEntity.badRequest().build();
        };
    }
}
