package com.risk.controller;
import com.risk.dto.ApiResponse;
import com.risk.entity.TradeTerminology;
import com.risk.mapper.TradeTerminologyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/terminology")
@CrossOrigin
public class TradeTerminologyController {
    @Autowired private TradeTerminologyMapper mapper;
    @GetMapping("/list")
    public ApiResponse<Map<String,Object>> list(@RequestParam(defaultValue="") String category, @RequestParam(defaultValue="") String keyword, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        List<TradeTerminology> list;
        if (category != null && !category.isEmpty()) {
            list = mapper.findByCategory(category, keyword, (page-1)*pageSize, pageSize);
        } else {
            list = mapper.search(keyword, (page-1)*pageSize, pageSize);
        }
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", mapper.countAll());
        return ApiResponse.success(data);
    }
    @PostMapping
    public ApiResponse<String> add(@RequestBody TradeTerminology term) {
        mapper.insert(term);
        return ApiResponse.success("添加成功");
    }
    @PutMapping("/{id}")
    public ApiResponse<String> update(@PathVariable Long id, @RequestBody TradeTerminology term) {
        term.setId(id);
        mapper.updateById(term);
        return ApiResponse.success("更新成功");
    }
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return ApiResponse.success("删除成功");
    }
}
