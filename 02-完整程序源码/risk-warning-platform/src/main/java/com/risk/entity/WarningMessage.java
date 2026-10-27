package com.risk.entity;

import lombok.Data;
import java.util.Date;

/**
 * 预警消息实体
 */
@Data
public class WarningMessage {
    private Long id;
    private String creditCode;          // 企业信用代码
    private String enterpriseName;      // 企业名称
    private String warningType;         // 预警类型（经营异常/行政处罚/失信名单/违规报关/贸易骤降）
    private String warningLevel;        // 预警等级（严重/高风险/中风险）
    private String content;             // 预警内容
    private String dataSource;          // 数据来源
    private Integer isRead;            // 是否已读 0=未读 1=已读
    private String status;             // 处理状态：未处理/处理中/已处理/已关闭
    private String handler;            // 处理人
    private Date handleTime;           // 处理时间
    private String handleResult;       // 处理说明
    private Date createTime;           // 创建时间
}
