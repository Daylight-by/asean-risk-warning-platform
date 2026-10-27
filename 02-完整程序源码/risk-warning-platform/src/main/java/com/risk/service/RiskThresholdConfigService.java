package com.risk.service;

import com.risk.entity.RiskThresholdConfig;
import com.risk.mapper.RiskThresholdConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RiskThresholdConfigService {

    @Autowired
    private RiskThresholdConfigMapper mapper;

    private volatile RiskThresholdConfig cached;

    public RiskThresholdConfig getConfig() {
        RiskThresholdConfig current = cached;
        if (current == null) {
            synchronized (this) {
                current = cached;
                if (current == null) {
                    current = mapper.findCurrent();
                    if (current == null) current = defaults();
                    cached = current;
                }
            }
        }
        return current;
    }

    public synchronized RiskThresholdConfig save(RiskThresholdConfig config) {
        validate(config);
        config.setId(1L);
        mapper.upsert(config);
        cached = mapper.findCurrent();
        return cached;
    }

    public RiskThresholdConfig reset() {
        return save(defaults());
    }

    private void validate(RiskThresholdConfig c) {
        requireRange("交易异常阈值", c.getTradeAbnormal(), 100, 500);
        requireRange("资金流水波动阈值", c.getCapitalFlow(), 10, 100);
        requireRange("关联风险阈值", c.getRelatedRisk(), 30, 100);
        requireRange("报关一致性阈值", c.getCustomsMatch(), 70, 100);
        requireRange("税务合规阈值", c.getTaxScore(), 50, 100);
        requireRange("高风险评分阈值", c.getHighRiskScore(), 0, 100);
        requireRange("中风险评分阈值", c.getMidRiskScore(), 0, 100);
        if (c.getHighRiskScore() >= c.getMidRiskScore()) {
            throw new IllegalArgumentException("高风险评分阈值必须小于中风险评分阈值");
        }
    }

    private void requireRange(String name, Double value, double min, double max) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(name + "必须在" + min + "到" + max + "之间");
        }
    }

    private RiskThresholdConfig defaults() {
        RiskThresholdConfig c = new RiskThresholdConfig();
        c.setId(1L);
        c.setTradeAbnormal(300.0);
        c.setCapitalFlow(50.0);
        c.setRelatedRisk(60.0);
        c.setCustomsMatch(90.0);
        c.setTaxScore(80.0);
        c.setHighRiskScore(40.0);
        c.setMidRiskScore(70.0);
        return c;
    }
}
