package com.risk.dto;

import lombok.Data;

/**
 * 可视化查询 —— 服务端白名单校验后生成的过滤条件。
 * column / sqlOp 均由服务端根据白名单映射，用户不可直接注入；
 * 仅 paramValue（用户传入的值）以 #{} 参数化方式绑定，杜绝 SQL 注入。
 */
@Data
public class QueryFilter {
    private String column;     // 校验通过的数据库列名（安全）
    private String sqlOp;      // 映射后的 SQL 操作符（= / != / LIKE / > / >= / < / <=）
    private Object paramValue;  // 用户传入的过滤值（参数化绑定）
}
