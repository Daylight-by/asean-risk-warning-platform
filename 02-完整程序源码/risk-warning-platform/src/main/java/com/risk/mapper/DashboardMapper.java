package com.risk.mapper;

import com.risk.entity.QueryDashboard;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 我的看板 Mapper（注解式 SQL，沿用项目现有风格）。
 */
@Mapper
public interface DashboardMapper {

    @Insert("INSERT INTO t_query_dashboard(username, name, config_json, create_time) " +
            "VALUES(#{username}, #{name}, #{configJson}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QueryDashboard d);

    @Select("SELECT id, username, name, config_json AS configJson, create_time AS createTime " +
            "FROM t_query_dashboard WHERE username = #{username} ORDER BY create_time DESC")
    List<QueryDashboard> selectByUsername(@Param("username") String username);

    @Select("SELECT id, username, name, config_json AS configJson, create_time AS createTime " +
            "FROM t_query_dashboard WHERE id = #{id}")
    QueryDashboard selectById(@Param("id") Long id);

    @Delete("DELETE FROM t_query_dashboard WHERE id = #{id} AND username = #{username}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("username") String username);
}
