package com.risk.mapper;

import com.risk.entity.CrossBorderRelation;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrossBorderRelationMapper {

    @Select("SELECT * FROM t_cross_border_relation WHERE source_credit_code = #{sourceCreditCode}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "sourceCreditCode", column = "source_credit_code"),
            @Result(property = "sourceEnterprise", column = "source_enterprise"),
            @Result(property = "sourceCountry", column = "source_country"),
            @Result(property = "targetEntityName", column = "target_entity_name"),
            @Result(property = "targetCountry", column = "target_country"),
            @Result(property = "targetEntityType", column = "target_entity_type"),
            @Result(property = "relationType", column = "relation_type"),
            @Result(property = "relationDepth", column = "relation_depth"),
            @Result(property = "riskLevel", column = "risk_level"),
            @Result(property = "riskTag", column = "risk_tag"),
            @Result(property = "evidenceSource", column = "evidence_source"),
            @Result(property = "createTime", column = "create_time")
    })
    List<CrossBorderRelation> selectBySourceCreditCode(@Param("sourceCreditCode") String sourceCreditCode);

    @Select("SELECT * FROM t_cross_border_relation WHERE risk_level = #{riskLevel}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "sourceCreditCode", column = "source_credit_code"),
            @Result(property = "sourceEnterprise", column = "source_enterprise"),
            @Result(property = "sourceCountry", column = "source_country"),
            @Result(property = "targetEntityName", column = "target_entity_name"),
            @Result(property = "targetCountry", column = "target_country"),
            @Result(property = "targetEntityType", column = "target_entity_type"),
            @Result(property = "relationType", column = "relation_type"),
            @Result(property = "relationDepth", column = "relation_depth"),
            @Result(property = "riskLevel", column = "risk_level"),
            @Result(property = "riskTag", column = "risk_tag"),
            @Result(property = "evidenceSource", column = "evidence_source"),
            @Result(property = "createTime", column = "create_time")
    })
    List<CrossBorderRelation> selectByRiskLevel(@Param("riskLevel") String riskLevel);

    @Select("SELECT target_country, COUNT(*) AS cnt FROM t_cross_border_relation GROUP BY target_country")
    List<Map<String, Object>> countGroupByTargetCountry();

    @Insert("INSERT INTO t_cross_border_relation " +
            "(source_credit_code, source_enterprise, source_country, target_entity_name, " +
            "target_country, target_entity_type, relation_type, relation_depth, " +
            "risk_level, risk_tag, evidence_source, create_time) " +
            "VALUES " +
            "(#{sourceCreditCode}, #{sourceEnterprise}, #{sourceCountry}, #{targetEntityName}, " +
            "#{targetCountry}, #{targetEntityType}, #{relationType}, #{relationDepth}, " +
            "#{riskLevel}, #{riskTag}, #{evidenceSource}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CrossBorderRelation relation);
}
