-- 我的看板（可视化查询配置持久化）
-- 字符集 utf8mb4，避免中文乱码
CREATE TABLE IF NOT EXISTS t_query_dashboard (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL DEFAULT 'admin',
    name        VARCHAR(128) NOT NULL,
    config_json TEXT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_dashboard_user (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
