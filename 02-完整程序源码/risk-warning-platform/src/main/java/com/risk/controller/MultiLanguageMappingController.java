package com.risk.controller;
import com.risk.dto.ApiResponse;
import com.risk.entity.MultiLanguageMapping;
import com.risk.mapper.MultiLanguageMappingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/multi-lang")
@CrossOrigin
public class MultiLanguageMappingController {
    @Autowired private MultiLanguageMappingMapper mapper;
    @GetMapping("/search")
    public ApiResponse<List<MultiLanguageMapping>> search(@RequestParam String keyword) {
        return ApiResponse.success(mapper.search(keyword));
    }
    @GetMapping("/{creditCode}")
    public ApiResponse<MultiLanguageMapping> get(@PathVariable String creditCode) {
        return ApiResponse.success(mapper.findByCreditCode(creditCode));
    }
    @PostMapping
    public ApiResponse<String> add(@RequestBody MultiLanguageMapping mapping) {
        mapper.insert(mapping);
        return ApiResponse.success("添加成功");
    }
}
