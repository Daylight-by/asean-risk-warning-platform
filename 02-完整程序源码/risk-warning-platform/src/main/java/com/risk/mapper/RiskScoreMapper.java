package com.risk.mapper;

import com.risk.entity.RiskScore;
import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface RiskScoreMapper {

    @Insert("INSERT INTO t_risk_score(credit_code, total_score, risk_level, score_date, score_detail) " +
            "VALUES(#{creditCode}, #{totalScore}, #{riskLevel}, #{scoreDate}, #{scoreDetail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RiskScore riskScore);

    @Select("SELECT total_score AS totalScore, risk_level AS riskLevel " +
            "FROM t_risk_score WHERE credit_code = #{creditCode} " +
            "ORDER BY create_time DESC LIMIT 1")
    Map<String, Object> findLatestByCreditCode(@Param("creditCode") String creditCode);
}
