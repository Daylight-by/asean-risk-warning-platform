package com.risk.mapper;

import com.risk.entity.ComplianceCheck;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ComplianceCheckMapper {

    @Select("SELECT * FROM t_compliance_check WHERE credit_code = #{creditCode}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "creditCode", column = "credit_code"),
            @Result(property = "enterpriseName", column = "enterprise_name"),
            @Result(property = "targetCountry", column = "target_country"),
            @Result(property = "checkStatus", column = "check_status"),
            @Result(property = "totalScore", column = "total_score"),
            @Result(property = "riskSummary", column = "risk_summary"),
            @Result(property = "qualificationCheck", column = "qualification_check"),
            @Result(property = "tariffRisk", column = "tariff_risk"),
            @Result(property = "customsRedline", column = "customs_redline"),
            @Result(property = "reportGenerated", column = "report_generated"),
            @Result(property = "createTime", column = "create_time")
    })
    List<ComplianceCheck> selectByCreditCode(@Param("creditCode") String creditCode);

    @Select("SELECT * FROM t_compliance_check WHERE target_country = #{targetCountry}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "creditCode", column = "credit_code"),
            @Result(property = "enterpriseName", column = "enterprise_name"),
            @Result(property = "targetCountry", column = "target_country"),
            @Result(property = "checkStatus", column = "check_status"),
            @Result(property = "totalScore", column = "total_score"),
            @Result(property = "riskSummary", column = "risk_summary"),
            @Result(property = "qualificationCheck", column = "qualification_check"),
            @Result(property = "tariffRisk", column = "tariff_risk"),
            @Result(property = "customsRedline", column = "customs_redline"),
            @Result(property = "reportGenerated", column = "report_generated"),
            @Result(property = "createTime", column = "create_time")
    })
    List<ComplianceCheck> selectByTargetCountry(@Param("targetCountry") String targetCountry);

    @Insert("INSERT INTO t_compliance_check " +
            "(credit_code, enterprise_name, target_country, check_status, total_score, " +
            "risk_summary, qualification_check, tariff_risk, customs_redline, " +
            "report_generated, create_time) " +
            "VALUES " +
            "(#{creditCode}, #{enterpriseName}, #{targetCountry}, #{checkStatus}, #{totalScore}, " +
            "#{riskSummary}, #{qualificationCheck}, #{tariffRisk}, #{customsRedline}, " +
            "#{reportGenerated}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ComplianceCheck complianceCheck);

    @Update("UPDATE t_compliance_check SET report_generated = #{reportGenerated} WHERE id = #{id}")
    int updateReportGenerated(@Param("id") Long id, @Param("reportGenerated") Integer reportGenerated);
}
