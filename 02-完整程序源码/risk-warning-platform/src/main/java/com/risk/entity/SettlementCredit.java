package com.risk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Alias("SettlementCredit")
public class SettlementCredit {

    private Long id;
    private String creditCode;
    private String enterpriseName;
    private String settlementType;
    private String settlementCurrency;
    private BigDecimal settlementAmount;
    private String settlementDirection;
    private String targetCountry;
    private LocalDate settlementDate;
    private Integer isAbnormal;
    private String abnormalReason;
    private Integer creditScore;
    private String dataSource;
    private LocalDateTime createTime;
}
