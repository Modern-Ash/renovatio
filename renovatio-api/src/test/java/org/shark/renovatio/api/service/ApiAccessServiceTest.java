package org.shark.renovatio.api.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.core.service.ReportAccessService;
import org.shark.renovatio.shared.domain.AccessRole;

import static org.junit.jupiter.api.Assertions.*;

class ApiAccessServiceTest {

    private final ReportAccessService reportAccessService = new ReportAccessService();
    private final ApiAccessService apiAccessService = new ApiAccessService(reportAccessService);

    @Test
    void adminCanDoEverything() {
        assertTrue(apiAccessService.canView(AccessRole.ADMIN));
        assertTrue(apiAccessService.canModify(AccessRole.ADMIN));
        assertTrue(apiAccessService.canCreate(AccessRole.ADMIN));
    }

    @Test
    void managerCanViewAndModify() {
        assertTrue(apiAccessService.canView(AccessRole.MANAGER));
        assertTrue(apiAccessService.canModify(AccessRole.MANAGER));
        assertFalse(apiAccessService.canCreate(AccessRole.MANAGER));
    }

    @Test
    void viewerCannotViewOrModify() {
        assertFalse(apiAccessService.canView(AccessRole.VIEWER));
        assertFalse(apiAccessService.canModify(AccessRole.VIEWER));
        assertFalse(apiAccessService.canCreate(AccessRole.VIEWER));
    }

    @Test
    void nullRoleCannotDoAnything() {
        assertFalse(apiAccessService.canView(null));
        assertFalse(apiAccessService.canModify(null));
        assertFalse(apiAccessService.canCreate(null));
    }
}
