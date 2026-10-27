package com.risk.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface PolicyMapper {

    @Select("<script>" +
            "SELECT * FROM t_policy WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND (policy_title LIKE CONCAT('%',#{keyword},'%') OR policy_content LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='country != null and country != \"\"'> AND target_country = #{country}</if>" +
            " ORDER BY publish_date DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String,Object>> searchPage(@Param("keyword") String keyword,
                                         @Param("country") String country,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_policy WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND (policy_title LIKE CONCAT('%',#{keyword},'%') OR policy_content LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='country != null and country != \"\"'> AND target_country = #{country}</if>" +
            "</script>")
    int countSearch(@Param("keyword") String keyword, @Param("country") String country);

    @Select("SELECT * FROM t_policy WHERE policy_category = #{category} ORDER BY publish_date DESC LIMIT 20")
    List<Map<String,Object>> findByCategory(@Param("category") String category);

    @Insert("INSERT INTO t_policy (policy_title, policy_category, target_country, publish_date, policy_content, data_source, link) " +
            "VALUES (#{policyTitle}, #{policyCategory}, #{targetCountry}, #{publishDate}, #{policyContent}, #{dataSource}, #{link})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String,Object> policy);

    @Update("UPDATE t_policy SET policy_title=#{policyTitle}, policy_category=#{policyCategory}, " +
            "target_country=#{targetCountry}, publish_date=#{publishDate}, policy_content=#{policyContent}, " +
            "data_source=#{dataSource}, link=#{link} WHERE id=#{id}")
    int update(Map<String,Object> policy);

    /** 局部更新：仅切换启用/禁用状态，避免全字段替换触发 NOT NULL 约束 */
    @Update("UPDATE t_policy SET is_active=#{isActive} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("isActive") int isActive);

    @Delete("DELETE FROM t_policy WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    /** 查询所有不重复的目标国家/地区（用于统计覆盖国家数） */
    @Select("SELECT DISTINCT target_country FROM t_policy WHERE target_country IS NOT NULL AND target_country != ''")
    List<String> findDistinctCountries();
}