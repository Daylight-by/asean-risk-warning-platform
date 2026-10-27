package com.risk.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 风控评分结果 DTO
 */
@Data
public class RiskScoreResult {
    private Double totalScore;           // 综合得分 0-100
    private String riskLevel;            // 高风险/中风险/低风险
    private Double basicScore;           // 基础资质得分
    private Double complianceScore;      // 经营合规得分
    private Double customsScore;         // 口岸通关得分
    private Double tradeScore;           // 贸易活跃得分
    private Double adjustScore;          // 综合调整得分
    private List<String> riskTags;       // 风险标签
    private Date calculateTime;          // 计算时间
}
