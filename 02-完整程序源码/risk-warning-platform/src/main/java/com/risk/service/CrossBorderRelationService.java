package com.risk.service;

import com.risk.entity.CrossBorderRelation;
import com.risk.mapper.CrossBorderRelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CrossBorderRelationService {

    @Autowired
    private CrossBorderRelationMapper crossBorderRelationMapper;

    /** 按信用代码查询关联关系 */
    public List<CrossBorderRelation> getRelationsByCreditCode(String creditCode) {
        return crossBorderRelationMapper.selectBySourceCreditCode(creditCode);
    }

    /** 查询高风险关联关系 */
    public List<CrossBorderRelation> getHighRiskRelations(String creditCode) {
        List<CrossBorderRelation> all = crossBorderRelationMapper.selectBySourceCreditCode(creditCode);
        return all.stream().filter(r -> "高".equals(r.getRiskLevel())).toList();
    }

    /** 获取关联关系统计（国家->数量） */
    public Map<String, Object> getRelationStats() {
        List<Map<String, Object>> dbData = crossBorderRelationMapper.countGroupByTargetCountry();
        Map<String, Object> stats = new LinkedHashMap<>();
        if (dbData != null) {
            for (Map<String, Object> row : dbData) {
                // MyBatis 的 Map 查询保留数据库列名 target_country，
                // 不会像实体映射一样自动转换为 targetCountry。
                Object countryValue = row.get("target_country");
                if (countryValue == null) countryValue = row.get("targetCountry");
                Object countValue = row.get("cnt");
                if (countryValue != null && countValue instanceof Number) {
                    stats.put(String.valueOf(countryValue), ((Number) countValue).intValue());
                }
            }
        }
        return stats;
    }
}
