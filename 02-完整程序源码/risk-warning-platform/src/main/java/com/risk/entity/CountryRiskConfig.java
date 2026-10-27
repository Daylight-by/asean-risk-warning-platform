package com.risk.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 分国别风控模型配置
 */
@Data
@Alias("CountryRiskConfig")
public class CountryRiskConfig {
    private Long id;
    private String countryCode;      // CN, VN, TH, MY, ID, KH, LA
    private String countryName;      // 中国, 越南, 泰国...
    private Double basicWeight;      // 基础资质权重
    private Double complianceWeight; // 经营合规权重
    private Double customsWeight;    // 口岸通关权重
    private Double tradeWeight;      // 贸易活跃权重
    private Double adjustWeight;     // 综合调整权重
    private String focusArea;        // 重点关注领域（JSON数组字符串）
    private String riskRules;        // 特殊风控规则（JSON字符串）
    private Integer isActive;        // 是否启用
}
