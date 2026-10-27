package com.risk.mapper;

import com.risk.entity.Enterprise;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface EnterpriseMapper {

    @Select("SELECT * FROM t_enterprise ORDER BY id ASC")
    List<Enterprise> findAll();

    @Select("SELECT * FROM t_enterprise WHERE credit_code = #{creditCode}")
    Enterprise findByCreditCode(@Param("creditCode") String creditCode);

    @Select("SELECT * FROM t_enterprise WHERE enterprise_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR credit_code = #{keyword} " +
            "OR credit_code IN (SELECT credit_code FROM t_multi_language_mapping WHERE name_cn LIKE CONCAT('%',#{keyword},'%') " +
            "OR name_en LIKE CONCAT('%',#{keyword},'%') OR name_vn LIKE CONCAT('%',#{keyword},'%') " +
            "OR name_th LIKE CONCAT('%',#{keyword},'%') OR name_my LIKE CONCAT('%',#{keyword},'%') " +
            "OR name_id LIKE CONCAT('%',#{keyword},'%') OR name_kh LIKE CONCAT('%',#{keyword},'%')) LIMIT 20")
    List<Enterprise> fuzzySearch(@Param("keyword") String keyword);

    @Select("SELECT * FROM t_enterprise WHERE primary_port = #{portName} LIMIT 100")
    List<Enterprise> findByPort(@Param("portName") String portName);

    @Select("SELECT COUNT(*) FROM t_enterprise")
    int countAll();

    @Select("SELECT industry_category, COUNT(*) as cnt FROM t_enterprise WHERE industry_category IS NOT NULL AND industry_category != '' GROUP BY industry_category ORDER BY cnt DESC LIMIT 10")
    List<Map<String,Object>> groupByIndustry();

    @Select("SELECT COUNT(DISTINCT credit_code) FROM t_enterprise WHERE primary_port = #{portName}")
    int countByPort(@Param("portName") String portName);

    /**
     * 查询企业及其风险评分
     */
    @Select("""
        SELECT 
            e.credit_code AS creditCode,
            e.enterprise_name AS enterpriseName,
            e.primary_port AS primaryPort,
            e.industry_category AS industryCategory,
            e.registered_capital AS registeredCapital,
            e.trade_volume AS tradeVolume,
            e.id,
            rs.total_score AS riskScore
        FROM t_enterprise e 
        LEFT JOIN (
            SELECT credit_code, total_score,
                   ROW_NUMBER() OVER (PARTITION BY credit_code ORDER BY create_time DESC) AS rn
            FROM t_risk_score
        ) rs ON e.credit_code = rs.credit_code AND rs.rn = 1
        ORDER BY e.id ASC
        """)
    List<Map<String, Object>> findAllWithRiskScore();
}
