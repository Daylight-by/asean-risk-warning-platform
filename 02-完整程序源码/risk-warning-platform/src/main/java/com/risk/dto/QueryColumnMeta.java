package com.risk.dto;

import lombok.Data;
import java.util.List;

/**
 * 可视化查询 —— 数据源可输出 / 可筛选字段的元信息（供前端渲染向导）。
 */
@Data
public class QueryColumnMeta {
    private String field;          // 数据库列名
    private String label;          // 中文显示名
    private String type;           // string / number / date / enum
    private Boolean groupable;     // 是否可作为图表分组维度
    private Boolean defaultDisplay; // 表格模式默认是否展示
    private List<String> options;  // 枚举类型的可选值
}
