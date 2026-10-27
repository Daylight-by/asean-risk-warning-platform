package com.risk.mapper;
import com.risk.entity.TradeTerminology;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;
@Mapper
public interface TradeTerminologyMapper {
    @Select("SELECT * FROM t_trade_terminology WHERE category = #{category} AND (term_cn LIKE CONCAT('%',#{keyword},'%') OR term_en LIKE CONCAT('%',#{keyword},'%') OR term_vn LIKE CONCAT('%',#{keyword},'%') OR term_th LIKE CONCAT('%',#{keyword},'%') OR term_my LIKE CONCAT('%',#{keyword},'%') OR term_in LIKE CONCAT('%',#{keyword},'%') OR term_km LIKE CONCAT('%',#{keyword},'%')) ORDER BY id LIMIT #{offset}, #{limit}")
    List<TradeTerminology> findByCategory(@Param("category") String category, @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    @Select("SELECT * FROM t_trade_terminology WHERE term_cn LIKE CONCAT('%',#{keyword},'%') OR term_en LIKE CONCAT('%',#{keyword},'%') OR term_vn LIKE CONCAT('%',#{keyword},'%') OR term_th LIKE CONCAT('%',#{keyword},'%') OR term_my LIKE CONCAT('%',#{keyword},'%') OR term_in LIKE CONCAT('%',#{keyword},'%') OR term_km LIKE CONCAT('%',#{keyword},'%') ORDER BY id LIMIT #{offset}, #{limit}")
    List<TradeTerminology> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    @Select("SELECT COUNT(*) FROM t_trade_terminology")
    int countAll();
    @Insert("INSERT INTO t_trade_terminology(term_cn,term_en,term_vn,term_th,term_my,term_in,term_km,category,hs_code,description,source) VALUES(#{termCn},#{termEn},#{termVn},#{termTh},#{termMy},#{termIn},#{termKm},#{category},#{hsCode},#{description},#{source})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TradeTerminology term);
    @Update("UPDATE t_trade_terminology SET term_cn=#{termCn},term_en=#{termEn},term_vn=#{termVn},term_th=#{termTh},term_my=#{termMy},term_in=#{termIn},term_km=#{termKm},category=#{category},hs_code=#{hsCode},description=#{description},source=#{source} WHERE id=#{id}")
    int updateById(TradeTerminology term);
    @Delete("DELETE FROM t_trade_terminology WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
