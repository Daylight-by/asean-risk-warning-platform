package com.risk.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 可视化查询 —— 执行请求体。
 * source    : 数据源 key
 * chartType : table / bar / line / pie
 * groupBy   : 图表模式下的分组字段（必须为可分组列）
 * columns   : 表格模式下的输出列（为空则使用默认展示列）
 * filters   : [{ field, op, value }]，field/op/value 均为用户原始输入，由服务端校验
 */
@Data
public class VisualQueryRequest {
    private String source;
    private String chartType;
    private String groupBy;
    private List<String> columns;
    private List<Map<String, String>> filters;
}
