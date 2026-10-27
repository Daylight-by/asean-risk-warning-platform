package com.risk.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface MultilingualContentMapper {
    @Select("SELECT business_id AS businessId, field_name AS fieldName, translated_text AS translatedText, " +
            "translation_status AS translationStatus, verification_status AS verificationStatus " +
            "FROM t_multilingual_content WHERE business_type=#{type} AND locale=#{locale} " +
            "AND FIND_IN_SET(business_id, #{ids})")
    List<Map<String,Object>> findByBusinessIds(@Param("type") String type, @Param("locale") String locale, @Param("ids") String ids);
}
