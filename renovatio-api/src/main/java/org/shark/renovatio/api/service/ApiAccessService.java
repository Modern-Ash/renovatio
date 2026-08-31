package org.shark.renovatio.api.service;

import org.shark.renovatio.core.service.ReportAccessService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessService {
    private final ReportAccessService reportAccessService;

    public ApiAccessService(ReportAccessService reportAccessService) {
        this.reportAccessService = reportAccessService;
    }

    public boolean canView(AccessRole role) {
        return reportAccessService.canView(role);
    }

    public boolean canModify(AccessRole role) {
        return role == AccessRole.ADMIN || role == AccessRole.MANAGER;
    }

    public boolean canCreate(AccessRole role) {
        return role == AccessRole.ADMIN;
    }
}
