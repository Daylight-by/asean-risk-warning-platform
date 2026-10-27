package com.risk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Alias("FraudAlert")
public class FraudAlert {

    private Long id;
    private String creditCode;
    private String enterpriseName;
    private String fraudType;
    private BigDecimal riskProbability;
    private String riskLevel;
    private String alertContent;
    private String modelFeatures;
    private String predictionPeriod;
    private Integer isConfirmed;
    private String handler;
    private String handleResult;
    private LocalDateTime handleTime;     // 处理时间（处置闭环）
    private String handleAction;          // 处置动作：冻结结算/降级信用/发送整改通知/核实无罪
    private String dataSource;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
