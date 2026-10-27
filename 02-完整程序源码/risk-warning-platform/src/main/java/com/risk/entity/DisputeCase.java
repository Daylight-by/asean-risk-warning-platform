package com.risk.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 纠纷案例库
 */
@Data
@Alias("DisputeCase")
public class DisputeCase {
    private Long id;
    private String caseNo;           // 案例编号
    private String title;            // 案例标题
    private String caseType;         // 类型：合同纠纷/知识产权/产品质量/支付纠纷
    private String tradeCategory;    // 贸易品类
    private String countries;        // 涉及国家
    private String summary;          // 案例摘要
    private String judgment;         // 判决结果
    private String riskAdvice;       // 风险建议
    private String court;            // 审理法院
    private String publishDate;      // 发布日期
    private String source;           // 数据来源
    private transient Double similarity;  // 匹配度 0-100（仅响应字段，由 Controller 计算后填充）
}
