package com.risk.entity;

import lombok.Data;
import java.util.Date;

/**
 * 可视化查询 —— 我的看板（后端持久化）。
 * 每个看板保存一份查询配置（JSON），按用户隔离。
 */
@Data
public class QueryDashboard {
    private Long id;
    private String username;
    private String name;
    private String configJson;
    private Date createTime;
}
