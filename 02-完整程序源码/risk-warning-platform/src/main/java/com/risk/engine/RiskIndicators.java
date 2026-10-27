package com.risk.engine;

import lombok.Data;

/**
 * 风险指标数据模型 - 聚合多源公共数据
 */
@Data
public class RiskIndicators {
    private String creditCode;
    private String enterpriseName;
    private double registeredCapital;
    private int establishYears;
    private String industryCategory;
    private String primaryPort;
    private int abnormalOperationCount;
    private int recentAbnormalCount;
    private int administrativePenaltyCount;
    private boolean isInDishonestyList;
    private boolean hasForeignTradeRecord;
    private boolean hasCrossBorderEcommerceRecord;
    private int customsViolationCount;
    private double inspectionAbnormalRate;
    private double customsTimeDeviation;
    private int recent12MonthDeclarationCount;
    private int tradeCategoryCount;
    private int continuousActiveMonths;

    // 分国别风控扩展字段
    private String targetCountry;              // 目标贸易国家
    private Double portClearanceAvgHours;      // 口岸通关平均时长（小时）
    private Integer inspectionFailCount;       // 查验不合格次数
    private Double borderTradeAmount;          // 边贸交易金额（万元）
}
