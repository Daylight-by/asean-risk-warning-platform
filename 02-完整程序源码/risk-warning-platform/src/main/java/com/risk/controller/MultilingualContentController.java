package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.mapper.MultilingualContentMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/i18n")
public class MultilingualContentController {
    private final MultilingualContentMapper mapper;
    public MultilingualContentController(MultilingualContentMapper mapper) { this.mapper = mapper; }

    @GetMapping("/content")
    public ApiResponse<Map<String,Object>> content(@RequestParam String type, @RequestParam String locale, @RequestParam String ids) {
        List<Map<String,Object>> rows = mapper.findByBusinessIds(type, locale, ids);
        Map<String,Object> data = new LinkedHashMap<>();
        for (Map<String,Object> row : rows) {
            String id = String.valueOf(row.get("businessId"));
            @SuppressWarnings("unchecked") Map<String,Object> fields = (Map<String,Object>) data.computeIfAbsent(id, k -> new LinkedHashMap<>());
            fields.put(String.valueOf(row.get("fieldName")), row.get("translatedText"));
            fields.put("translationStatus", row.get("translationStatus"));
            fields.put("verificationStatus", row.get("verificationStatus"));
        }
        return ApiResponse.success(data);
    }
}
