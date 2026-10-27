USE risk_warning;

CREATE TABLE IF NOT EXISTS t_datasource_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE COMMENT 'datasource type: customs/tax/business',
    name VARCHAR(100) NOT NULL COMMENT 'datasource name',
    url VARCHAR(500) NOT NULL COMMENT 'endpoint url',
    api_key VARCHAR(300) DEFAULT '' COMMENT 'api key, optional for demo',
    status VARCHAR(50) DEFAULT '已配置' COMMENT 'config status',
    remark VARCHAR(500) DEFAULT '' COMMENT 'remark',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='external public datasource config';
