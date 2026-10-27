package com.risk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.entity.QueryDashboard;
import com.risk.mapper.DashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 我的看板服务：按用户隔离地保存/读取/删除可视化查询配置。
 * config 以 JSON 字符串落库，读取时还原为 Map 返回前端。
 */
@Service
public class DashboardService {

    @Autowired
    private DashboardMapper mapper;

    private static final ObjectMapper JSON = new ObjectMapper();

    public List<Map<String, Object>> list(String username) {
        List<QueryDashboard> rows = mapper.selectByUsername(username);
        List<Map<String, Object>> result = new ArrayList<>();
        for (QueryDashboard d : rows) {
            result.add(toView(d));
        }
        return result;
    }

    public Map<String, Object> save(String username, String name, Object config) {
        QueryDashboard d = new QueryDashboard();
        d.setUsername(username);
        d.setName(name == null || name.trim().isEmpty() ? "未命名看板" : name.trim());
        try {
            d.setConfigJson(JSON.writeValueAsString(config));
        } catch (Exception e) {
            d.setConfigJson("{}");
        }
        mapper.insert(d);
        return toView(d);
    }

    public void delete(String username, Long id) {
        mapper.deleteByIdAndUser(id, username);
    }

    private Map<String, Object> toView(QueryDashboard d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("createTime", d.getCreateTime());
        Object cfg = new LinkedHashMap<>();
        try {
            if (d.getConfigJson() != null && !d.getConfigJson().isEmpty()) {
                cfg = JSON.readValue(d.getConfigJson(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
            // 解析失败时回退为空配置，前端以默认值兜底
        }
        m.put("config", cfg);
        return m;
    }
}
