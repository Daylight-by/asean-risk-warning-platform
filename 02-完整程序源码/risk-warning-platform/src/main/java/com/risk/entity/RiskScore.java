package com.risk.entity;

import lombok.Data;
import java.util.Date;

/**
 * 风控评分持久化实体
 */
@Data
public class RiskScore {
    private Long id;
    private String creditCode;         // 统一社会信用代码
    private Double totalScore;          // 综合得分
    private String riskLevel;           // 风险等级
    private Date scoreDate;             // 评分日期
    private String scoreDetail;         // 评分明细JSON
    private Date createTime;            // 创建时间
}
