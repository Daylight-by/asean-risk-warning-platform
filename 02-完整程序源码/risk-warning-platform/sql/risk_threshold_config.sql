SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_threshold_config (
    id                  BIGINT PRIMARY KEY,
    trade_abnormal      DECIMAL(6,2) NOT NULL DEFAULT 300.00 COMMENT '单日交易金额超出月均值百分比',
    capital_flow        DECIMAL(6,2) NOT NULL DEFAULT 50.00 COMMENT '近3个月资金变动幅度百分比',
    related_risk        DECIMAL(6,2) NOT NULL DEFAULT 60.00 COMMENT '关联企业风险指数阈值',
    customs_match       DECIMAL(6,2) NOT NULL DEFAULT 90.00 COMMENT '报关与物流数据匹配度最低百分比',
    tax_score           DECIMAL(6,2) NOT NULL DEFAULT 80.00 COMMENT '税务合规最低分',
    high_risk_score     DECIMAL(6,2) NOT NULL DEFAULT 40.00 COMMENT '综合评分小于等于该值为高风险',
    mid_risk_score      DECIMAL(6,2) NOT NULL DEFAULT 70.00 COMMENT '综合评分小于等于该值为中风险',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控阈值配置表';

INSERT INTO t_risk_threshold_config
  (id, trade_abnormal, capital_flow, related_risk, customs_match, tax_score, high_risk_score, mid_risk_score)
VALUES (1, 300, 50, 60, 90, 80, 40, 70)
ON DUPLICATE KEY UPDATE id = id;
