package com.risk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Alias("CrossBorderRelation")
public class CrossBorderRelation {

    private Long id;
    private String sourceCreditCode;
    private String sourceEnterprise;
    private String sourceCountry;
    private String targetEntityName;
    private String targetCountry;
    private String targetEntityType;
    private String relationType;
    private Integer relationDepth;
    private String riskLevel;
    private String riskTag;
    private String evidenceSource;
    private LocalDateTime createTime;
}
