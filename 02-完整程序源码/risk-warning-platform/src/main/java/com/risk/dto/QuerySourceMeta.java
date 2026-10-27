package com.risk.dto;

import lombok.Data;
import java.util.List;

/**
 * 可视化查询 —— 数据源元信息（供前端渲染数据源下拉与字段选择）。
 */
@Data
public class QuerySourceMeta {
    private String key;
    private String name;
    private String description;
    private List<QueryColumnMeta> columns;
}
