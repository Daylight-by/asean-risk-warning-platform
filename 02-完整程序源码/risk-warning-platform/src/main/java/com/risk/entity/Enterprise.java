package com.risk.entity;

import lombok.Data;
import java.util.Date;

/**
 * 企业基础信息实体（当前为演示样例字段，正式接入后记录权威来源）
 */
@Data
public class Enterprise {
    private Long id;
    private String creditCode;         // 统一社会信用代码
    private String enterpriseName;      // 企业名称
    private String legalPerson;         // 法定代表人
    private Double registeredCapital;   // 注册资本（万元）
    private Date establishDate;         // 成立日期
    private Integer establishYears;     // 成立年限（计算字段）
    private String industryCategory;    // 行业类别
    private String businessScope;       // 经营范围
    private String primaryPort;         // 主要关联口岸
    private Double tradeVolume;         // 贸易额（万元）
    private Date createTime;
    private Date updateTime;
}
