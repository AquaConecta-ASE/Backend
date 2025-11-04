package com.ironcoders.aquaconectabackend.dashboard.interfaces;

import com.ironcoders.aquaconectabackend.dashboard.domain.model.dto.DashboardSummaryDto;
import com.ironcoders.aquaconectabackend.dashboard.domain.services.DashboardQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    public DashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public DashboardSummaryDto getDashboardResumen() {
        return dashboardQueryService.getDashboard();
    }
}
