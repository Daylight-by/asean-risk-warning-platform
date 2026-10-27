package com.risk.mapper;

import com.risk.entity.RiskThresholdConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RiskThresholdConfigMapper {

    @Select("SELECT * FROM t_risk_threshold_config WHERE id = 1")
    RiskThresholdConfig findCurrent();

    @Insert("INSERT INTO t_risk_threshold_config(" +
            "id, trade_abnormal, capital_flow, related_risk, customs_match, tax_score, high_risk_score, mid_risk_score" +
            ") VALUES(1, #{tradeAbnormal}, #{capitalFlow}, #{relatedRisk}, #{customsMatch}, #{taxScore}, #{highRiskScore}, #{midRiskScore}) " +
            "ON DUPLICATE KEY UPDATE trade_abnormal=VALUES(trade_abnormal), capital_flow=VALUES(capital_flow), " +
            "related_risk=VALUES(related_risk), customs_match=VALUES(customs_match), tax_score=VALUES(tax_score), " +
            "high_risk_score=VALUES(high_risk_score), mid_risk_score=VALUES(mid_risk_score), update_time=NOW()")
    int upsert(RiskThresholdConfig config);
}
