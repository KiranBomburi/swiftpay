package com.swiftpay.analytics.controller;

import com.swiftpay.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/volume")
    public ResponseEntity<Map<String, Object>> getVolumeSummary() {
        // TODO: add date range filter later, right now returns all-time stats
        return ResponseEntity.ok(analyticsService.getVolumeSummary());
    }
}
