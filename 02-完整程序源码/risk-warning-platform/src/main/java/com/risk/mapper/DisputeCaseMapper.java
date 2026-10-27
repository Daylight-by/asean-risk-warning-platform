package com.risk.mapper;
import com.risk.entity.DisputeCase;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DisputeCaseMapper {
    @Select("SELECT * FROM t_dispute_case WHERE case_type = #{caseType} AND (title LIKE CONCAT('%',#{keyword},'%') OR summary LIKE CONCAT('%',#{keyword},'%')) ORDER BY publish_date DESC LIMIT #{offset}, #{limit}")
    List<DisputeCase> findByType(@Param("caseType") String caseType, @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    @Select("SELECT * FROM t_dispute_case WHERE title LIKE CONCAT('%',#{keyword},'%') OR summary LIKE CONCAT('%',#{keyword},'%') OR trade_category LIKE CONCAT('%',#{keyword},'%') ORDER BY publish_date DESC LIMIT #{offset}, #{limit}")
    List<DisputeCase> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    /** 智能匹配候选查询（宽松 OR）：贸易品类（含 title/summary）或涉及国家任一匹配即纳入候选
     *  由 Controller 对候选集计算多维加权相似度并排序，阈值过滤后返回 Top10
     *  空字符串会生成 '%%' 匹配全部（前端已校验至少一字段非空） */
    @Select("SELECT * FROM t_dispute_case " +
            "WHERE ( trade_category LIKE CONCAT('%',#{tradeCategory},'%') " +
            "     OR title            LIKE CONCAT('%',#{tradeCategory},'%') " +
            "     OR summary          LIKE CONCAT('%',#{tradeCategory},'%') " +
            "     OR countries        LIKE CONCAT('%',#{countries},'%') ) " +
            "ORDER BY publish_date DESC LIMIT 50")
    List<DisputeCase> findCandidates(@Param("tradeCategory") String tradeCategory, @Param("countries") String countries);
    @Select("SELECT * FROM t_dispute_case WHERE id = #{id}")
    DisputeCase findById(@Param("id") Long id);
    @Select("SELECT COUNT(*) FROM t_dispute_case")
    int countAll();
    @Insert("INSERT INTO t_dispute_case(case_no,title,case_type,trade_category,countries,summary,judgment,risk_advice,court,publish_date,source) VALUES(#{caseNo},#{title},#{caseType},#{tradeCategory},#{countries},#{summary},#{judgment},#{riskAdvice},#{court},#{publishDate},#{source})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DisputeCase caseItem);
}
