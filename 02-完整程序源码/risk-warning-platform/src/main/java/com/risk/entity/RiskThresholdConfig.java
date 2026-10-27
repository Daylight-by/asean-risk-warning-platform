package com.risk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class RiskThresholdConfig {
    private Long id;
    private Double tradeAbnormal;
    private Double capitalFlow;
    private Double relatedRisk;
    private Double customsMatch;
    private Double taxScore;
    private Double highRiskScore;
    private Double midRiskScore;
    private Date updateTime;
}
