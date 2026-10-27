package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.RiskThresholdConfig;
import com.risk.service.RiskThresholdConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/thresholds")
public class RiskThresholdConfigController {

    @Autowired
    private RiskThresholdConfigService service;

    @GetMapping
    public ApiResponse<RiskThresholdConfig> get() {
        return ApiResponse.success(service.getConfig());
    }

    @PutMapping
    public ApiResponse<RiskThresholdConfig> save(@RequestBody RiskThresholdConfig config) {
        try {
            return ApiResponse.success(service.save(config));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/reset")
    public ApiResponse<RiskThresholdConfig> reset() {
        return ApiResponse.success(service.reset());
    }
}
