package com.risk.controller;
import com.risk.dto.ApiResponse;
import com.risk.entity.CountryRiskConfig;
import com.risk.mapper.CountryRiskConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/country-risk")
@CrossOrigin
public class CountryRiskConfigController {
    @Autowired private CountryRiskConfigMapper mapper;
    @GetMapping("/configs")
    public ApiResponse<List<CountryRiskConfig>> list() {
        return ApiResponse.success(mapper.findAll());
    }
    @GetMapping("/{countryCode}")
    public ApiResponse<CountryRiskConfig> get(@PathVariable String countryCode) {
        return ApiResponse.success(mapper.findByCode(countryCode));
    }
    @PostMapping
    public ApiResponse<String> add(@RequestBody CountryRiskConfig config) {
        mapper.insert(config);
        return ApiResponse.success("添加成功");
    }
    @PutMapping("/{id}")
    public ApiResponse<String> update(@PathVariable Long id, @RequestBody CountryRiskConfig config) {
        config.setId(id);
        mapper.updateById(config);
        return ApiResponse.success("更新成功");
    }
}
