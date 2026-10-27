package com.risk.mapper;

import com.risk.dto.QueryFilter;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * 可视化查询 —— 通用、白名单驱动的查询 Mapper。
 *
 * 安全说明：
 *  - table / columns / groupByClause 全部由服务端基于白名单拼接（非用户输入），使用 ${} 安全替换；
 *  - 过滤条件的值通过 #{f.paramValue} 参数化绑定，杜绝 SQL 注入；
 *  - 列名 (f.column) 与操作符 (f.sqlOp) 同样来自服务端白名单映射。
 */
@Mapper
public interface VisualQueryMapper {

    @Select("<script>" +
            "SELECT ${columns} FROM ${table} " +
            "<where>" +
            "<foreach collection='filters' item='f' open='' close=''>" +
            " AND ${f.column} ${f.sqlOp} #{f.paramValue} " +
            "</foreach>" +
            "</where> ${groupByClause}" +
            "</script>")
    List<Map<String, Object>> query(@Param("columns") String columns,
                                     @Param("table") String table,
                                     @Param("filters") List<QueryFilter> filters,
                                     @Param("groupByClause") String groupByClause);
}
