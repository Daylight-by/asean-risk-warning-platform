package com.risk.mapper;
import com.risk.entity.MultiLanguageMapping;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface MultiLanguageMappingMapper {
    @Select("SELECT * FROM t_multi_language_mapping WHERE credit_code = #{creditCode}")
    MultiLanguageMapping findByCreditCode(@Param("creditCode") String creditCode);
    @Select("SELECT * FROM t_multi_language_mapping WHERE name_cn LIKE CONCAT('%',#{keyword},'%') OR name_en LIKE CONCAT('%',#{keyword},'%') OR name_vn LIKE CONCAT('%',#{keyword},'%') OR name_th LIKE CONCAT('%',#{keyword},'%') OR name_my LIKE CONCAT('%',#{keyword},'%') OR name_id LIKE CONCAT('%',#{keyword},'%') OR name_kh LIKE CONCAT('%',#{keyword},'%') LIMIT 20")
    List<MultiLanguageMapping> search(@Param("keyword") String keyword);
    @Insert("INSERT INTO t_multi_language_mapping(credit_code,name_cn,name_en,name_vn,name_th,name_my,name_id,name_kh,source_country) VALUES(#{creditCode},#{nameCn},#{nameEn},#{nameVn},#{nameTh},#{nameMy},#{nameId},#{nameKh},#{sourceCountry})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MultiLanguageMapping mapping);
}
