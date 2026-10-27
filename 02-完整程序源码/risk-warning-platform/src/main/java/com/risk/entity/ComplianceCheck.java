package com.risk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Alias("ComplianceCheck")
public class ComplianceCheck {

    private Long id;
    private String creditCode;
    private String enterpriseName;
    private String targetCountry;
    private String checkStatus;
    private Integer totalScore;
    private String riskSummary;
    private String qualificationCheck;
    private String tariffRisk;
    private String customsRedline;
    private Integer reportGenerated;
    private LocalDateTime createTime;
}
