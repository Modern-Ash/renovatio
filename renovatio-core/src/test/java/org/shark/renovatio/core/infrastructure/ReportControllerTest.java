package org.shark.renovatio.core.infrastructure;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.shark.renovatio.core.service.MigrationReportService;
import org.shark.renovatio.core.service.ReportAccessService;
import org.shark.renovatio.shared.domain.MigrationReport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportControllerTest {

    @Test
    void getHtmlReport_forbidden_whenRoleNotAllowed() {
        MigrationReportService reportService = mock(MigrationReportService.class);
        ReportAccessService accessService = mock(ReportAccessService.class);
        when(accessService.canView(null)).thenReturn(false);

        ReportController controller = new ReportController(reportService, accessService);
        ResponseEntity<String> resp = controller.getHtmlReport(null);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertNull(resp.getBody());
        verifyNoInteractions(reportService);
    }

    @Test
    void getHtmlReport_ok_whenAllowed() {
        MigrationReportService reportService = mock(MigrationReportService.class);
        ReportAccessService accessService = mock(ReportAccessService.class);
        when(accessService.canView(any())).thenReturn(true);
        MigrationReport mr = new MigrationReport();
        when(reportService.aggregateReport()).thenReturn(mr);
        when(reportService.renderHtml(mr)).thenReturn("<html>ok</html>");

        ReportController controller = new ReportController(reportService, accessService);
        ResponseEntity<String> resp = controller.getHtmlReport("ADMIN");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("<html>ok</html>", resp.getBody());
    }

    @Test
    void getPdfReport_forbidden_whenRoleNotAllowed() {
        MigrationReportService reportService = mock(MigrationReportService.class);
        ReportAccessService accessService = mock(ReportAccessService.class);
        when(accessService.canView(null)).thenReturn(false);

        ReportController controller = new ReportController(reportService, accessService);
        ResponseEntity<byte[]> resp = controller.getPdfReport(null);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertNull(resp.getBody());
        verifyNoInteractions(reportService);
    }

    @Test
    void getPdfReport_ok_whenAllowed() {
        MigrationReportService reportService = mock(MigrationReportService.class);
        ReportAccessService accessService = mock(ReportAccessService.class);
        when(accessService.canView(any())).thenReturn(true);
        MigrationReport mr = new MigrationReport();
        when(reportService.aggregateReport()).thenReturn(mr);
        byte[] pdf = new byte[]{1,2,3};
        when(reportService.renderPdf(mr)).thenReturn(pdf);

        ReportController controller = new ReportController(reportService, accessService);
        ResponseEntity<byte[]> resp = controller.getPdfReport("MANAGER");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertArrayEquals(pdf, resp.getBody());
        assertTrue(resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("report.pdf"));
    }
}

