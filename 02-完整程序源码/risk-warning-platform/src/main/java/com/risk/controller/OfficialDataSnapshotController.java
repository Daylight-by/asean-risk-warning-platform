package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.mapper.OfficialDataSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evidence-first public data API.
 * It intentionally exposes only aggregate official statistics, never enterprise-level data.
 */
@RestController
@RequestMapping("/api/official-data")
public class OfficialDataSnapshotController {

    @Autowired
    private OfficialDataSnapshotMapper snapshotMapper;

    @GetMapping("/snapshots")
    public ApiResponse<List<Map<String, Object>>> snapshots(
            @RequestParam(defaultValue = "") String datasetCode,
            @RequestParam(defaultValue = "") String countryCode,
            @RequestParam(defaultValue = "100") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : snapshotMapper.findSnapshots(datasetCode.trim(), countryCode.trim().toUpperCase(), safeLimit)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("sourceName", row.get("source_name"));
            item.put("sourceUrl", row.get("source_url"));
            item.put("datasetCode", row.get("dataset_code"));
            item.put("metricKey", row.get("metric_key"));
            item.put("countryCode", row.get("country_code"));
            item.put("countryName", row.get("country_name"));
            item.put("referencePeriod", row.get("reference_period"));
            item.put("metricValue", row.get("metric_value"));
            item.put("unit", row.get("unit"));
            item.put("dataStatus", row.get("data_status"));
            item.put("retrievedAt", row.get("retrieved_at"));
            item.put("contentSha256", row.get("content_sha256"));
            item.put("provenanceNote", row.get("provenance_note"));
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/sources")
    public ApiResponse<List<Map<String, Object>>> sources() {
        return ApiResponse.success(snapshotMapper.summarizeSources());
    }
}
