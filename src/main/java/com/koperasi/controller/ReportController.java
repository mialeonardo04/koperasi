package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.dto.ReportDto;
import com.koperasi.entity.User;
import com.koperasi.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ReportDto.DashboardMember>> getDashboardMember(
            @AuthenticationPrincipal User user) {
        ReportDto.DashboardMember dashboard = reportService.getDashboardMember(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }
}