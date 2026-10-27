package com.risk.mapper;
import com.risk.entity.CountryRiskConfig;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface CountryRiskConfigMapper {
    @Select("SELECT * FROM t_country_risk_config WHERE is_active = 1 ORDER BY id")
    List<CountryRiskConfig> findAll();
    @Select("SELECT * FROM t_country_risk_config WHERE country_code = #{countryCode}")
    CountryRiskConfig findByCode(@Param("countryCode") String countryCode);
    @Insert("INSERT INTO t_country_risk_config(country_code,country_name,basic_weight,compliance_weight,customs_weight,trade_weight,adjust_weight,focus_area,risk_rules,is_active) VALUES(#{countryCode},#{countryName},#{basicWeight},#{complianceWeight},#{customsWeight},#{tradeWeight},#{adjustWeight},#{focusArea},#{riskRules},#{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CountryRiskConfig config);
    @Update("<script>" +
            "UPDATE t_country_risk_config " +
            "<set>" +
            "<if test='countryCode != null'>country_code=#{countryCode},</if>" +
            "<if test='countryName != null'>country_name=#{countryName},</if>" +
            "<if test='basicWeight != null'>basic_weight=#{basicWeight},</if>" +
            "<if test='complianceWeight != null'>compliance_weight=#{complianceWeight},</if>" +
            "<if test='customsWeight != null'>customs_weight=#{customsWeight},</if>" +
            "<if test='tradeWeight != null'>trade_weight=#{tradeWeight},</if>" +
            "<if test='adjustWeight != null'>adjust_weight=#{adjustWeight},</if>" +
            "<if test='focusArea != null'>focus_area=#{focusArea},</if>" +
            "<if test='riskRules != null'>risk_rules=#{riskRules},</if>" +
            "<if test='isActive != null'>is_active=#{isActive},</if>" +
            "</set>" +
            "WHERE id=#{id}" +
            "</script>")
    int updateById(CountryRiskConfig config);
}
