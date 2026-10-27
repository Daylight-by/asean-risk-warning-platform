-- ============================================
-- 东盟跨境中小商户智能风控预警平台
-- 数据库初始化脚本 v1.0
-- MySQL 8.0+
-- 数据状态：比赛脱敏演示样例；字段口径参考公开数据目录，正式接入后逐项补充授权依据
-- ============================================

-- 关键：docker mysql 镜像默认 client 是 latin1，必须显式声明 utf8mb4，
-- 否则 UTF-8 字节会被双重编码成"乱码字符串"（æŠ¥å…³å• 这种）
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS risk_warning
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE risk_warning;

-- ============================================
-- 1. 企业基础信息表
-- 拟接入来源：广西市场监管公开数据
-- ============================================
CREATE TABLE IF NOT EXISTS t_enterprise (
                                            id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            credit_code         VARCHAR(18)  NOT NULL UNIQUE COMMENT '统一社会信用代码',
    enterprise_name     VARCHAR(200) NOT NULL COMMENT '企业名称',
    legal_person        VARCHAR(100) COMMENT '法定代表人',
    registered_capital  DECIMAL(15,2) COMMENT '注册资本（万元）',
    establish_date      DATE         COMMENT '成立日期',
    establish_years     INT          COMMENT '成立年限（计算字段）',
    industry_category   VARCHAR(100) COMMENT '行业类别',
    business_scope      TEXT         COMMENT '经营范围',
    primary_port        VARCHAR(50)  COMMENT '主要关联口岸',
    trade_volume        DECIMAL(15,2) COMMENT '年交易额（万元）',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name      (enterprise_name(50)),
    INDEX idx_credit    (credit_code),
    INDEX idx_industry  (industry_category(50))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业基础信息表（当前为演示样例）';

-- ============================================
-- 2. 经营异常记录表
-- 拟接入来源：广西市场监管公开数据
-- ============================================
CREATE TABLE IF NOT EXISTS t_abnormal_operation (
                                                    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    credit_code     VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    abnormal_type   VARCHAR(100) COMMENT '异常类型',
    list_date       DATE         COMMENT '列入日期',
    remove_date     DATE         COMMENT '移出日期',
    list_reason     TEXT         COMMENT '列入原因',
    data_source     VARCHAR(200) DEFAULT '项目演示样例（拟接入市场监管公开数据）' COMMENT '数据来源',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit (credit_code),
    INDEX idx_list_date (list_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经营异常记录表（当前为演示样例）';

-- ============================================
-- 3. 行政处罚记录表
-- 拟接入来源：广西市场监管公开数据
-- ============================================
CREATE TABLE IF NOT EXISTS t_administrative_penalty (
                                                        id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                        credit_code     VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    penalty_type    VARCHAR(100) COMMENT '处罚类型',
    penalty_date    DATE         COMMENT '处罚日期',
    penalty_reason  TEXT         COMMENT '处罚原因',
    data_source     VARCHAR(200) DEFAULT '项目演示样例（拟接入市场监管公开数据）' COMMENT '数据来源',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit (credit_code),
    INDEX idx_penalty_date (penalty_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政处罚记录表（当前为演示样例）';

-- ============================================
-- 4. 口岸通关记录表
-- 拟接入来源：广西口岸通关公开统计
-- ============================================
CREATE TABLE IF NOT EXISTS t_customs_declaration (
                                                     id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     credit_code           VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    enterprise_name       VARCHAR(200) COMMENT '企业名称',
    port_name             VARCHAR(50)  COMMENT '口岸名称',
    declaration_type      VARCHAR(20)  COMMENT '申报类型（进口/出口）',
    declaration_date      DATE         COMMENT '申报日期',
    goods_category        VARCHAR(100) COMMENT '商品品类',
    is_violation          TINYINT DEFAULT 0 COMMENT '是否违规报关 0=否 1=是',
    inspection_result     VARCHAR(20)  COMMENT '查验结果',
    clearance_duration   INT          COMMENT '通关时长（小时）',
    declared_value       DECIMAL(16,2) NOT NULL DEFAULT 0 COMMENT '申报货值（人民币元，项目演示样例）',
    data_source           VARCHAR(200) DEFAULT '项目演示样例（拟接入口岸通关公开统计）' COMMENT '数据来源',
    create_time           DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit      (credit_code),
    INDEX idx_port_date   (port_name, declaration_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口岸通关记录表（口岸办公开统计）';

-- ============================================
-- 5. 对外贸易备案表
-- 拟接入来源：广西商务公开数据
-- ============================================
CREATE TABLE IF NOT EXISTS t_foreign_trade_record (
                                                      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                      credit_code     VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    record_type     VARCHAR(50)  COMMENT '备案类型（外贸经营者/跨境电商）',
    record_date     DATE         COMMENT '备案日期',
    record_status   VARCHAR(20)  COMMENT '备案状态',
    data_source     VARCHAR(200) DEFAULT '项目演示样例（拟接入商务公开数据）' COMMENT '数据来源',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit (credit_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对外贸易备案表（当前为演示样例）';

-- ============================================
-- 6. 预警消息表
-- ============================================
CREATE TABLE IF NOT EXISTS t_warning_message (
                                                 id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 credit_code       VARCHAR(18)  NOT NULL COMMENT '企业信用代码',
    enterprise_name   VARCHAR(200) COMMENT '企业名称',
    warning_type      VARCHAR(50)  COMMENT '预警类型',
    warning_level     VARCHAR(20)  COMMENT '预警等级',
    warning_content   TEXT         COMMENT '预警内容',
    data_source       VARCHAR(200) COMMENT '数据来源',
    is_read           TINYINT DEFAULT 0 COMMENT '是否已读 0=未读 1=已读',
    status            VARCHAR(20) NOT NULL DEFAULT '未处理' COMMENT '处理状态：未处理/处理中/已处理/已关闭',
    handler           VARCHAR(100) COMMENT '处理人',
    handle_time       DATETIME     COMMENT '处理时间',
    handle_result     TEXT         COMMENT '处理说明',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit  (credit_code),
    INDEX idx_create_time (create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警消息表';

-- ============================================
-- 7. 预警订阅表
-- ============================================
CREATE TABLE IF NOT EXISTS t_warning_subscription (
                                                      id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                      open_id     VARCHAR(100) NOT NULL COMMENT '用户微信OpenID',
    credit_code VARCHAR(18)  NOT NULL COMMENT '订阅企业信用代码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_openid_credit (open_id, credit_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警订阅表';

-- ============================================
-- 7.1 外部公共数据源配置表
-- ============================================
CREATE TABLE IF NOT EXISTS t_datasource_config (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   type VARCHAR(50) NOT NULL UNIQUE COMMENT '数据源类型：customs/tax/business',
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    url VARCHAR(500) NOT NULL COMMENT '接口地址',
    api_key VARCHAR(300) DEFAULT '' COMMENT '接口密钥，演示环境可为空',
    status VARCHAR(50) DEFAULT '已配置' COMMENT '配置状态',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部公共数据源配置表';

-- ============================================
-- 7.2 官方公共数据快照表
-- 只保存可公开复核的聚合统计；企业级演示明细不写入此表。
-- 真实快照数据由 sql/official_public_data.sql 导入。
-- ============================================
CREATE TABLE IF NOT EXISTS t_public_data_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(160) NOT NULL,
    source_url VARCHAR(700) NOT NULL,
    dataset_code VARCHAR(100) NOT NULL,
    metric_key VARCHAR(120) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    country_name VARCHAR(100) NOT NULL,
    reference_period VARCHAR(32) NOT NULL,
    metric_value DECIMAL(24,6) NOT NULL,
    unit VARCHAR(80) NOT NULL,
    data_status VARCHAR(32) NOT NULL DEFAULT 'official_public',
    retrieved_at DATETIME NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    provenance_note VARCHAR(500) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_official_snapshot (dataset_code, metric_key, country_code, reference_period),
    KEY idx_snapshot_country (country_code),
    KEY idx_snapshot_source (source_name),
    KEY idx_snapshot_period (reference_period)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官方公共数据快照（聚合统计，含来源追溯）';

-- ============================================
-- 7. 企业风控评分表
-- 数据来源：风控引擎计算结果
-- ============================================
CREATE TABLE IF NOT EXISTS t_risk_score (
                                            id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            credit_code     VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    total_score     DECIMAL(5,2) COMMENT '综合评分（0-100）',
    risk_level      VARCHAR(20)  COMMENT '风险等级（高/中/低）',
    score_date      DATE         COMMENT '评分日期',
    score_detail    TEXT         COMMENT '评分明细JSON',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit (credit_code),
    INDEX idx_risk_level (risk_level),
    INDEX idx_score_date (score_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业风控评分表';

-- ============================================
-- 8. 广西口岸地理信息表
-- 拟接入来源：公开口岸地理信息
-- ============================================
CREATE TABLE IF NOT EXISTS t_port_geo (
                                          id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          port_name     VARCHAR(50)  NOT NULL COMMENT '口岸名称',
    port_level    VARCHAR(20)  COMMENT '口岸级别（一类/二类）',
    port_type     VARCHAR(20)  COMMENT '口岸类型（陆路/水运/空港）',
    longitude     DECIMAL(10,6) COMMENT '经度',
    latitude      DECIMAL(10,6) COMMENT '纬度',
    border_country VARCHAR(50) DEFAULT '越南' COMMENT '接壤国家',
    data_source   VARCHAR(200) DEFAULT '项目演示样例（地理字段参考公开信息）' COMMENT '数据来源',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广西口岸地理信息表（当前为演示样例）';

-- ============================================
-- 9. 东盟边贸政策库表
-- ============================================
CREATE TABLE IF NOT EXISTS t_policy (
                                        id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        policy_title    VARCHAR(500) NOT NULL COMMENT '政策标题',
    policy_category VARCHAR(50)  COMMENT '政策分类（关税/准入/流程/优惠）',
    target_country  VARCHAR(50)  COMMENT '目标国家',
    publish_date    DATE         COMMENT '发布日期',
    policy_content  TEXT         COMMENT '政策内容',
    data_source     VARCHAR(200) COMMENT '数据来源',
    is_active       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 0禁用 1启用',
    link            VARCHAR(500) COMMENT '附件链接',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (policy_category),
    INDEX idx_country  (target_country)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='东盟边贸政策库表';

-- 初始演示政策数据：用于首次启动时保证政策查询、首页统计和监管看板有完整闭环。
-- 这些记录是项目演示样例，正式部署时应替换为经过核验的公开政策数据。
INSERT INTO t_policy (policy_title, policy_category, target_country, publish_date, policy_content, data_source, is_active)
SELECT * FROM (
    SELECT '中国-东盟自贸区关税减让与原产地规则说明' AS policy_title, '关税' AS policy_category, '东盟多国' AS target_country, '2025-01-01' AS publish_date, '用于演示目标国家关税与原产地规则查询的政策摘要。' AS policy_content, '项目演示样例' AS data_source, 1 AS is_active
    UNION ALL SELECT '越南水果进口检验检疫要求', '准入', '越南', '2025-03-15', '用于演示越南农产品进口资质、原产地证明和检疫材料提示。', '项目演示样例', 1
    UNION ALL SELECT '凭祥口岸通关便利化措施', '流程', '越南', '2024-12-01', '用于演示口岸提前申报、两步申报和鲜活农产品通关提示。', '项目演示样例', 1
    UNION ALL SELECT '边境小额贸易税收优惠政策摘要', '优惠', '东盟多国', '2025-02-28', '用于演示边境小额贸易优惠政策与适用条件提示。', '项目演示样例', 1
    UNION ALL SELECT '东兴国家重点开发开放试验区政策摘要', '优惠', '越南', '2024-06-30', '用于演示广西沿边开放平台政策检索与企业合规提醒。', '项目演示样例', 1
    UNION ALL SELECT '中国-东盟跨境电商商品准入提示', '准入', '东盟多国', '2025-01-20', '用于演示跨境电商商品准入、交易限值和材料准备提醒。', '项目演示样例', 1
    UNION ALL SELECT '南宁海关通关一体化改革摘要', '流程', '东盟多国', '2024-09-15', '用于演示通关一体化、无纸化申报和申报口岸选择提示。', '项目演示样例', 1
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM t_policy WHERE policy_title = seed.policy_title);

-- ============================================
-- 10. 跨境关联关系表（创新：跨境多层关联网络）
-- 拟接入来源：工商、海关与边贸代理公开信息
-- ============================================
CREATE TABLE IF NOT EXISTS t_cross_border_relation (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_credit_code  VARCHAR(18)  NOT NULL COMMENT '源企业统一社会信用代码',
    source_enterprise   VARCHAR(200) COMMENT '源企业名称',
    source_country      VARCHAR(50)  DEFAULT '中国' COMMENT '源企业所在国',
    target_entity_name  VARCHAR(200) NOT NULL COMMENT '关联实体名称（支持多语种）',
    target_country      VARCHAR(50)  NOT NULL COMMENT '关联实体所在国',
    target_entity_type  VARCHAR(50)  COMMENT '关联实体类型（企业/法人/代持人/货代/供货商）',
    relation_type       VARCHAR(100) NOT NULL COMMENT '关联类型（股权关联/法人关联/代理中间商/上下游供货商/跨境货代）',
    relation_depth      INT          DEFAULT 1 COMMENT '关联层级（1=直接,2=二层,3=三层以上）',
    risk_level          VARCHAR(20)  DEFAULT '中' COMMENT '关联风险等级（高/中/低）',
    risk_tag            VARCHAR(200) COMMENT '风险标签（联合套利/虚假转口/资金分流/空壳公司）',
    evidence_source     VARCHAR(500) COMMENT '关联证据来源描述',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source    (source_credit_code),
    INDEX idx_country   (target_country),
    INDEX idx_risk      (risk_level),
    INDEX idx_depth     (relation_depth)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨境关联关系表（工商穿透+海关+边贸数据融合）';

-- ============================================
-- 11. 跨境结算信用表（创新：边贸结算信用分）
-- 拟接入来源：跨境人民币结算公开统计
-- ============================================
CREATE TABLE IF NOT EXISTS t_settlement_credit (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_code         VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    enterprise_name     VARCHAR(200) COMMENT '企业名称',
    settlement_type     VARCHAR(50)  NOT NULL COMMENT '结算类型（跨境收汇/跨境付汇/退税/跨境担保）',
    settlement_currency VARCHAR(10)  DEFAULT 'CNY' COMMENT '结算币种',
    settlement_amount   DECIMAL(18,2) COMMENT '结算金额（万元）',
    settlement_direction VARCHAR(20) COMMENT '资金方向（收入/支出）',
    target_country      VARCHAR(50)  COMMENT '对手方所在国',
    settlement_date     DATE         COMMENT '结算日期',
    is_abnormal         TINYINT      DEFAULT 0 COMMENT '是否异常 0=正常 1=异常',
    abnormal_reason     VARCHAR(500) COMMENT '异常原因（金额突变/频率异常/对手方高风险）',
    credit_score        INT          COMMENT '结算信用评分（0-100）',
    data_source         VARCHAR(200) DEFAULT '项目演示样例（拟接入跨境结算公开统计）' COMMENT '数据来源',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit    (credit_code),
    INDEX idx_date      (settlement_date),
    INDEX idx_country   (target_country),
    INDEX idx_abnormal  (is_abnormal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨境结算信用表（跨境人民币结算公示数据）';

-- ============================================
-- 12. 欺诈预警记录表（创新：事前预测而非事后展示）
-- 数据状态：演示规则与时序特征生成，不作为真实执法结论
-- ============================================
CREATE TABLE IF NOT EXISTS t_fraud_alert (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_code         VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    enterprise_name     VARCHAR(200) COMMENT '企业名称',
    fraud_type          VARCHAR(100) NOT NULL COMMENT '欺诈类型（虚假出口退税/低值高报/高值低报/地下钱庄资金流转）',
    risk_probability    DECIMAL(5,4) COMMENT '预测风险概率（0-1）',
    risk_level          VARCHAR(20)  NOT NULL COMMENT '风险等级（红色预警/橙色预警/黄色关注）',
    alert_content       TEXT         COMMENT '预警详情描述',
    model_features      TEXT         COMMENT '关键特征（JSON格式：货值波动率/合作国变更/流水变化等）',
    prediction_period   VARCHAR(50)  COMMENT '预测时间窗口（近3个月/近6个月）',
    is_confirmed        TINYINT      DEFAULT 0 COMMENT '是否已核实 0=待核实 1=已确认 2=已排除',
    handler             VARCHAR(100) COMMENT '处理人',
    handle_result       VARCHAR(500) COMMENT '处理结果',
    handle_time         DATETIME     COMMENT '处理时间（处置闭环）',
    handle_action       VARCHAR(50)  COMMENT '处置动作：冻结结算/降级信用/发送整改通知/核实无罪',
    data_source         VARCHAR(200) DEFAULT '项目演示模型输出' COMMENT '数据来源',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_credit    (credit_code),
    INDEX idx_fraud     (fraud_type),
    INDEX idx_level     (risk_level),
    INDEX idx_confirmed (is_confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='欺诈预警记录表（LSTM时序预测模型）';

-- ============================================
-- 13. 合规体检记录表（创新：中小微边贸企业合规自助）
-- ============================================
CREATE TABLE IF NOT EXISTS t_compliance_check (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_code         VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    enterprise_name     VARCHAR(200) COMMENT '企业名称',
    target_country      VARCHAR(50)  NOT NULL COMMENT '目标贸易国家',
    check_status        VARCHAR(20)  DEFAULT '进行中' COMMENT '体检状态（进行中/已完成/已过期）',
    total_score         INT          COMMENT '合规总分（0-100）',
    risk_summary        TEXT         COMMENT '风险摘要',
    qualification_check TEXT         COMMENT '资质检查结果（JSON）',
    tariff_risk         TEXT         COMMENT '关税风险提示（JSON）',
    customs_redline     TEXT         COMMENT '报关红线提示（JSON）',
    report_generated    TINYINT      DEFAULT 0 COMMENT '是否已生成报告 0=否 1=是',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit    (credit_code),
    INDEX idx_country   (target_country)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规体检记录表（中小微边贸企业合规自助）';

-- ============================================
-- 初始化测试数据：广西主要边境口岸
-- ============================================
INSERT INTO t_port_geo (port_name, port_level, port_type, longitude, latitude, border_country)
VALUES
    ('凭祥口岸',   '一类', '陆路', 106.7600, 22.1100, '越南'),
    ('友谊关口岸', '一类', '陆路', 106.7500, 22.1300, '越南'),
    ('东兴口岸',   '一类', '陆路', 107.9500, 21.5500, '越南'),
    ('水口口岸',   '一类', '陆路', 106.5800, 22.5000, '越南'),
    ('龙邦口岸',   '一类', '陆路', 106.3800, 23.1300, '越南'),
    ('防城港口岸', '一类', '水运', 108.3500, 21.6800, '越南'),
    ('北海口岸',   '一类', '水运', 109.1200, 21.4800, '东盟多国'),
    ('南宁空港',   '一类', '空港', 108.1700, 22.6100, '东盟多国');

-- ============================================
-- 初始化数据：比赛演示企业样例（仅用于功能演示，不作为企业事实认定）
-- 统一社会信用代码均来自公开工商查询平台
-- ============================================
INSERT INTO t_enterprise (credit_code, enterprise_name, legal_person, registered_capital, establish_date, establish_years, industry_category, business_scope, primary_port)
VALUES
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '陈志强', 200.00, '2020-07-21', 6, '进出口贸易', '日用百货、电子产品、服装鞋帽、水产品、农副产品进出口', '东兴口岸'),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '李国华', 100.00, '2018-03-23', 8, '批发业', '水产品、海产品、农副产品的批发及进出口贸易', '东兴口岸'),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '王伟', 300.00, '2021-03-23', 5, '批发业', '矿产品、机械设备、五金电器、建材的批发零售及进出口', '东兴口岸'),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '黄小明', 500.00, '2019-06-15', 7, '食品加工', '东南亚进口食品加工、水果干、坚果、水产品的加工及进出口', '东兴口岸'),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '刘德明', 800.00, '2022-01-10', 4, '国际物流', '国际货物运输代理、报关报检服务、仓储服务、跨境电商物流', '东兴口岸'),
    ('91450681MA5KL40R01', '柳州市振宇金属贸易有限公司东兴市分公司', '张伟', 50.00, '2017-09-05', 9, '金属贸易', '金属材料、矿产品、化工产品的批发零售及进出口', '东兴口岸'),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '吴小龙', 600.00, '2018-08-12', 8, '物流运输', '普通货物运输、冷链物流、跨境物流、仓储配送服务', '东兴口岸'),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '林志明', 150.00, '2020-11-18', 6, '跨境电商', '跨境电商平台运营、东南亚水果线上批发、互联网技术服务', '东兴口岸');

-- ============================================
-- 测试数据：跨境关联关系（基于真实企业）
-- ============================================
INSERT INTO t_cross_border_relation (source_credit_code, source_enterprise, target_entity_name, target_country, target_entity_type, relation_type, relation_depth, risk_level, risk_tag, evidence_source)
VALUES
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', 'Dongxing Trading Vietnam Co.Ltd', '越南', '企业', '股权关联', 1, '中', NULL, '工商登记穿透'),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', 'Nguyen Thi Lan', '越南', '代持人', '法人关联', 2, '高', '联合套利', '海关申报记录交叉比对'),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', 'Thai Seafood Export Corp', '泰国', '上下游供货商', '股权关联', 1, '低', NULL, '边贸备案数据'),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', 'Ming Da Foods Co.Ltd', '越南', '企业', '股权关联', 1, '中', NULL, '工商登记穿透'),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', 'Phnom Penh Mining Ltd', '柬埔寨', '企业', '代理中间商', 2, '高', '矿产品转口', '口岸通关异常标记'),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', 'ASEAN Logistics Pte.Ltd', '新加坡', '企业', '股权关联', 1, '低', NULL, '工商登记穿透'),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', 'Durian Farm Vietnam', '越南', '上下游供货商', '供应链关联', 1, '中', NULL, '电商平台供应商审核'),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', 'Lao Transport Co.Ltd', '老挝', '企业', '代理中间商', 2, '中', '跨境运输', '海关备案数据');

-- ============================================
-- 测试数据：跨境结算信用
-- ============================================
INSERT INTO t_settlement_credit (credit_code, enterprise_name, settlement_type, settlement_amount, settlement_direction, target_country, settlement_date, is_abnormal, credit_score)
VALUES
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '跨境收汇', 150.00, '收入', '越南', '2026-01-15', 0, 82),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '跨境付汇', 80.00, '支出', '越南', '2026-02-20', 0, 82),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '跨境收汇', 45.00, '收入', '泰国', '2026-01-10', 0, 75),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '跨境付汇', 200.00, '支出', '越南', '2026-02-05', 0, 70),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '跨境收汇', 500.00, '收入', '柬埔寨', '2026-03-01', 1, 45),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '跨境付汇', 120.00, '支出', '新加坡', '2026-01-20', 0, 88),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '跨境收汇', 85.00, '收入', '越南', '2026-03-10', 0, 68),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '跨境付汇', 95.00, '支出', '老挝', '2026-02-28', 0, 72);

-- ============================================
-- 测试数据：欺诈预警记录
-- ============================================
INSERT INTO t_fraud_alert (credit_code, enterprise_name, fraud_type, risk_probability, risk_level, alert_content, model_features, prediction_period)
VALUES
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '虚假出口退税', 0.8234, '红色预警', '近3个月报关货值环比增长320%，与实际物流数据严重不匹配', '{"货值波动率":3.2,"合作国变更":"柬埔寨→越南→柬埔寨","流水变化":"快进快出"}', '近6个月'),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '低值高报', 0.4521, '黄色关注', '申报单价偏离同品类市场均价1.8个标准差', '{"价格偏离度":1.8,"品类":"电子产品","历史偏差趋势":"上升"}', '近3个月'),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '关联交易异常', 0.6102, '橙色预警', '与越南供应商交易频率异常，单笔金额集中在申报阈值以下', '{"交易频率":47,"单笔均值":4.8,"阈值规避概率":0.73}', '近3个月');

-- ============================================
-- 测试数据：合规体检记录
-- ============================================
INSERT INTO t_compliance_check (credit_code, enterprise_name, target_country, check_status, total_score, risk_summary)
VALUES
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '越南', '已完成', 78, '资质齐全，注意越南最新原产地证格式变更要求'),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '泰国', '已完成', 65, '缺少泰国FDA食品安全许可，进口食品加工需额外资质'),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '柬埔寨', '已完成', 52, '矿产品出口需提供柬埔寨环保部门许可，当前资质不全'),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '新加坡', '已完成', 91, '资质齐全，新加坡海关AEO互认有效'),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '越南', '已完成', 70, '跨境电商备案有效，越南水果进口检疫许可需续期');

-- ============================================
-- 测试数据：经营异常记录
-- 数据来源：广西市场监管局公开数据（模拟补充）
-- ============================================
INSERT INTO t_abnormal_operation (credit_code, abnormal_type, list_date, remove_date, list_reason, data_source)
VALUES
    ('91450681MA5QCDGM2X', '未按规定公示年度报告', '2024-07-01', '2024-09-15', '未在2024年6月30日前公示2023年度报告，经催告后补报', '广西市场监管局公开数据'),
    ('91450681MA5PGMT001', '通过登记的住所无法联系', '2025-03-10', NULL, '双随机抽查发现企业注册地无人办公，无法取得联系', '广西市场监管局公开数据'),
    ('91450681MA5N37BC0U', '公示信息隐瞒真实情况', '2023-08-20', '2024-01-10', '年报中隐瞒对外投资信息，经核查后更正', '广西市场监管局公开数据'),
    ('91450681MA5KL40R01', '未按规定公示年度报告', '2025-07-01', NULL, '未在2025年6月30日前公示2024年度报告', '广西市场监管局公开数据'),
    ('91450681315843133T', '通过登记的住所无法联系', '2024-11-05', '2025-02-20', '实地核查发现实际经营地址与注册地址不一致，已变更', '广西市场监管局公开数据'),
    ('91450681MA5N77DT41', '公示信息隐瞒真实情况', '2023-06-15', '2023-12-01', '未如实公示股东出资信息，经责令改正后已补报', '广西市场监管局公开数据'),
    ('91450681MA5PPA1D5N', '未按规定公示即时信息', '2025-01-20', '2025-03-05', '股权变更信息未在20个工作日内公示', '广西市场监管局公开数据');

-- ============================================
-- 测试数据：行政处罚记录
-- 数据来源：广西市场监管局公开数据（模拟补充）
-- ============================================
INSERT INTO t_administrative_penalty (credit_code, penalty_type, penalty_date, penalty_reason, data_source)
VALUES
    ('91450681MA5QCDGM2X', '罚款', '2024-09-20', '报关单申报不实，影响海关统计准确性，罚款人民币3万元', '广西市场监管局公开数据'),
    ('91450681MA5PGMT001', '警告', '2025-04-15', '跨境电商平台未按规定核验入驻商家资质，责令限期整改', '广西市场监管局公开数据'),
    ('91450681315843133T', '罚款', '2024-06-10', '进口食品标签不符合国家标准，罚款人民币1.5万元', '广西市场监管局公开数据'),
    ('91450681MA5N37BC0U', '罚款', '2023-12-05', '水产品检验检疫证明过期仍进行销售，罚款人民币2万元', '广西市场监管局公开数据'),
    ('91450681MAA79FX799', '警告', '2025-02-28', '国际物流货物申报信息与实际不符，责令整改', '广西市场监管局公开数据'),
    ('91450681MA5KL40R01', '罚款', '2024-03-18', '矿产品进出口未办理相关许可证，罚款人民币5万元', '广西市场监管局公开数据'),
    ('91450681MA5N77DT41', '罚款', '2023-10-25', '冷链运输车辆温度记录不完整，罚款人民币0.8万元', '广西市场监管局公开数据'),
    ('91450681MA5PPA1D5N', '警告', '2025-05-10', '进出口商品归类申报错误，责令改正', '广西市场监管局公开数据'),
    ('91450681MA5QCDGM2X', '没收违法所得', '2025-01-08', '以次充好出口矿产品，没收违法所得人民币12万元', '广西市场监管局公开数据'),
    ('91450681MA5PGMT001', '罚款', '2024-11-22', '跨境电商零售进口商品未加贴中文标签，罚款人民币1万元', '广西市场监管局公开数据'),
    ('91450681315843133T', '警告', '2025-06-01', '食品加工车间卫生条件不达标，责令限期整改', '广西市场监管局公开数据'),
    ('91450681MA5N37BC0U', '罚款', '2025-03-30', '海产品冷链运输温度不达标，罚款人民币1.2万元', '广西市场监管局公开数据'),
    ('91450681MAA79FX799', '罚款', '2024-08-15', '报关代理服务中协助客户低报价格，罚款人民币8万元', '广西市场监管局公开数据');

-- ============================================
-- 测试数据：口岸通关记录
-- 数据来源：广西口岸办公开统计（模拟补充）
-- ============================================
INSERT INTO t_customs_declaration (credit_code, enterprise_name, port_name, declaration_type, declaration_date, goods_category, is_violation, inspection_result, clearance_duration)
VALUES
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '东兴口岸', '出口', '2026-01-05', '电子产品', 0, '正常放行', 4),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '东兴口岸', '进口', '2026-01-12', '水产品', 0, '正常放行', 6),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '东兴口岸', '出口', '2026-02-08', '服装鞋帽', 0, '正常放行', 3),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '东兴口岸', '进口', '2026-02-20', '农副产品', 1, '查验异常', 18),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '东兴口岸', '出口', '2026-03-15', '日用百货', 0, '正常放行', 5),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '东兴口岸', '进口', '2026-01-08', '海产品', 0, '正常放行', 8),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '东兴口岸', '出口', '2026-01-25', '水产品', 0, '正常放行', 5),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '东兴口岸', '进口', '2026-02-14', '冷冻海鲜', 1, '温度异常', 24),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '东兴口岸', '出口', '2026-03-02', '加工水产品', 0, '正常放行', 4),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '东兴口岸', '出口', '2026-01-10', '矿产品', 0, '正常放行', 6),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '东兴口岸', '进口', '2026-01-22', '机械设备', 0, '正常放行', 10),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '东兴口岸', '出口', '2026-02-05', '矿产品', 1, '货值异常', 36),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '东兴口岸', '出口', '2026-02-28', '五金电器', 0, '正常放行', 5),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '东兴口岸', '进口', '2026-03-10', '建材', 1, '申报不实', 48),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '东兴口岸', '出口', '2026-03-20', '矿产品', 1, '货值异常', 72),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '东兴口岸', '进口', '2026-01-06', '水果干', 0, '正常放行', 8),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '东兴口岸', '进口', '2026-01-18', '坚果', 0, '正常放行', 6),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '东兴口岸', '出口', '2026-02-02', '加工食品', 0, '正常放行', 5),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '东兴口岸', '进口', '2026-02-15', '水产品', 1, '标签不合格', 20),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '东兴口岸', '进口', '2026-03-05', '热带水果', 0, '正常放行', 7),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '东兴口岸', '出口', '2026-01-03', '物流货物', 0, '正常放行', 3),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '东兴口岸', '进口', '2026-01-15', '物流货物', 0, '正常放行', 4),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '防城港口岸', '出口', '2026-02-10', '物流货物', 0, '正常放行', 5),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '东兴口岸', '进口', '2026-02-25', '物流货物', 1, '申报信息不符', 16),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '防城港口岸', '出口', '2026-03-12', '物流货物', 0, '正常放行', 4),
    ('91450681MA5KL40R01', '柳州市振宇金属贸易有限公司东兴市分公司', '东兴口岸', '出口', '2026-01-09', '金属材料', 0, '正常放行', 6),
    ('91450681MA5KL40R01', '柳州市振宇金属贸易有限公司东兴市分公司', '东兴口岸', '进口', '2026-01-20', '矿产品', 1, '许可证缺失', 30),
    ('91450681MA5KL40R01', '柳州市振宇金属贸易有限公司东兴市分公司', '东兴口岸', '出口', '2026-02-12', '化工产品', 0, '正常放行', 5),
    ('91450681MA5KL40R01', '柳州市振宇金属贸易有限公司东兴市分公司', '东兴口岸', '进口', '2026-03-08', '金属材料', 0, '正常放行', 7),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '东兴口岸', '出口', '2026-01-04', '冷链货物', 0, '正常放行', 4),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '东兴口岸', '进口', '2026-01-16', '冷链货物', 0, '正常放行', 5),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '防城港口岸', '出口', '2026-02-06', '普通货物', 0, '正常放行', 3),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '东兴口岸', '进口', '2026-02-22', '冷链货物', 1, '温度记录异常', 22),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '东兴口岸', '出口', '2026-03-14', '跨境物流货物', 0, '正常放行', 4),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '东兴口岸', '进口', '2026-01-07', '热带水果', 0, '正常放行', 8),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '东兴口岸', '进口', '2026-01-21', '榴莲', 0, '正常放行', 6),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '东兴口岸', '进口', '2026-02-04', '山竹', 1, '检疫不合格', 28),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '东兴口岸', '进口', '2026-02-18', '龙眼', 0, '正常放行', 5),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '东兴口岸', '进口', '2026-03-06', '火龙果', 0, '正常放行', 7),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '东兴口岸', '进口', '2026-03-18', '芒果', 1, '商家资质缺失', 32),
    ('91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '凭祥口岸', '出口', '2026-03-22', '电子产品', 0, '正常放行', 5),
    ('91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '凭祥口岸', '进口', '2026-03-18', '海产品', 0, '正常放行', 7),
    ('91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '友谊关口岸', '出口', '2026-03-25', '矿产品', 1, '货值异常', 40),
    ('91450681315843133T', '东兴市中越泰国际食品有限公司', '凭祥口岸', '进口', '2026-03-20', '坚果', 0, '正常放行', 6),
    ('91450681MAA79FX799', '东兴市安保达国际物流有限公司', '南宁空港', '出口', '2026-03-16', '物流货物', 0, '正常放行', 3),
    ('91450681MA5KL40R01', '柳州市振宇金属贸易有限公司东兴市分公司', '友谊关口岸', '出口', '2026-03-22', '金属材料', 0, '正常放行', 5),
    ('91450681MA5N77DT41', '广西东兴市隆发物流有限公司', '南宁空港', '进口', '2026-03-19', '冷链货物', 0, '正常放行', 4),
    ('91450681MA5PGMT001', '东兴市庆龙网络科技有限公司', '凭祥口岸', '进口', '2026-03-24', '热带水果', 0, '正常放行', 6);

-- ============================================
-- 测试数据：对外贸易备案
-- 数据来源：广西商务厅公开数据（模拟补充）
-- ============================================
INSERT INTO t_foreign_trade_record (credit_code, record_type, record_date, record_status)
VALUES
    ('91450681MA5PPA1D5N', '外贸经营者', '2020-08-15', '有效'),
    ('91450681MA5PPA1D5N', '跨境电商', '2021-03-20', '有效'),
    ('91450681MA5N37BC0U', '外贸经营者', '2018-05-10', '有效'),
    ('91450681MA5QCDGM2X', '外贸经营者', '2021-06-08', '有效'),
    ('91450681315843133T', '外贸经营者', '2019-08-22', '有效'),
    ('91450681315843133T', '跨境电商', '2020-11-15', '有效'),
    ('91450681MAA79FX799', '外贸经营者', '2022-03-05', '有效'),
    ('91450681MAA79FX799', '跨境电商', '2022-06-18', '有效'),
    ('91450681MA5KL40R01', '外贸经营者', '2017-11-20', '有效'),
    ('91450681MA5N77DT41', '外贸经营者', '2018-10-08', '有效'),
    ('91450681MA5N77DT41', '跨境电商', '2020-05-12', '有效'),
    ('91450681MA5PGMT001', '外贸经营者', '2021-01-25', '有效'),
    ('91450681MA5PGMT001', '跨境电商', '2021-04-10', '有效');

COMMIT;

-- ============================================
-- 分国别风控配置表
-- ============================================
CREATE TABLE IF NOT EXISTS t_country_risk_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code VARCHAR(10) NOT NULL UNIQUE COMMENT '国家代码',
    country_name VARCHAR(50) NOT NULL COMMENT '国家名称',
    basic_weight DECIMAL(3,2) DEFAULT 0.20 COMMENT '基础资质权重',
    compliance_weight DECIMAL(3,2) DEFAULT 0.30 COMMENT '经营合规权重',
    customs_weight DECIMAL(3,2) DEFAULT 0.25 COMMENT '口岸通关权重',
    trade_weight DECIMAL(3,2) DEFAULT 0.15 COMMENT '贸易活跃权重',
    adjust_weight DECIMAL(3,2) DEFAULT 0.10 COMMENT '综合调整权重',
    focus_area TEXT COMMENT '重点关注领域',
    risk_rules TEXT COMMENT '特殊风控规则',
    is_active INT DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分国别风控配置表';

INSERT INTO t_country_risk_config(country_code,country_name,basic_weight,compliance_weight,customs_weight,trade_weight,adjust_weight,focus_area,risk_rules) VALUES
('CN','中国',0.20,0.30,0.25,0.15,0.10,'国内工商合规、税务申报','标准风控模型'),
('VN','越南',0.15,0.25,0.30,0.15,0.15,'海关原产地证、工贸证合规、出口许可证','越南对原产地证要求严格，虚假原产地证将面临高额罚款'),
('TH','泰国',0.20,0.25,0.25,0.15,0.15,'转口贸易合规、BOI投资促进资质','泰国转口贸易需特别注意中转国原产地证明'),
('MY','马来西亚',0.20,0.30,0.20,0.15,0.15,'进出口配额、棕榈油等大宗商品资质、清真认证','马来西亚对棕榈油、橡胶等商品有出口配额限制'),
('ID','印度尼西亚',0.15,0.30,0.25,0.15,0.15,'外资持股限制、镍矿等资源出口合规、SNI认证','印尼对外资持股比例有严格限制，部分行业禁止外资进入'),
('KH','柬埔寨',0.25,0.20,0.25,0.15,0.15,'边贸小额备案、服装纺织品出口资质','柬埔寨服装出口享受欧盟EBA优惠关税，需关注合规'),
('LA','老挝',0.25,0.20,0.25,0.15,0.15,'边境贸易资质、矿产出口许可','老挝矿产出口需特别注意环保合规要求'),
('TL','东帝汶',0.25,0.20,0.25,0.15,0.15,'边贸小额备案、原油与咖啡出口资质、跨境结算合规','东帝汶2025年正式加入东盟，贸易体量小、合规体系在建，重点关注边贸真实性与跨境结算合规');

-- ============================================
-- 多语种企业名映射表
-- ============================================
CREATE TABLE IF NOT EXISTS t_multi_language_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_code VARCHAR(18) NOT NULL UNIQUE COMMENT '统一社会信用代码',
    name_cn VARCHAR(200) COMMENT '中文名称',
    name_en VARCHAR(200) COMMENT '英文名称',
    name_vn VARCHAR(200) COMMENT '越南语名称',
    name_th VARCHAR(200) COMMENT '泰语名称',
    name_my VARCHAR(200) COMMENT '马来语名称',
    name_id VARCHAR(200) COMMENT '印尼语名称',
    name_kh VARCHAR(200) COMMENT '柬埔寨语名称',
    source_country VARCHAR(50) COMMENT '来源国',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多语种企业名映射表';

-- ============================================
-- 跨境商事术语库表
-- ============================================
CREATE TABLE IF NOT EXISTS t_trade_terminology (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_cn VARCHAR(200) NOT NULL COMMENT '中文术语',
    term_en VARCHAR(200) COMMENT '英文术语',
    term_vn VARCHAR(200) COMMENT '越南语术语',
    term_th VARCHAR(200) COMMENT '泰语术语',
    term_my VARCHAR(200) COMMENT '马来语术语',
    term_in VARCHAR(200) COMMENT '印尼语术语',
    term_km VARCHAR(200) COMMENT '柬埔寨语术语',
    category VARCHAR(50) COMMENT '分类：报关/税务/物流/支付/法律',
    hs_code VARCHAR(20) COMMENT 'HS编码',
    description TEXT COMMENT '说明',
    source VARCHAR(200) COMMENT '数据来源',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨境商事术语库';

INSERT INTO t_trade_terminology(term_cn,term_en,term_vn,term_th,category,hs_code,description,source) VALUES
('报关单','Customs Declaration','Tờ khai hải quan','ใบขนสินค้า','报关','',"进出口货物向海关申报的法定单据",'海关总署'),
('原产地证','Certificate of Origin','Giấy chứng nhận xuất xứ','หนังสือรับรองถิ่นกำเนิด','报关','',"证明货物原产国的官方文件",'广西商务厅'),
('边境小额贸易','Border Small-scale Trade','Buôn bán nhỏ lẻ biên giới','การค้าชายแดนขนาดเล็ก','报关','',"沿边地区企业与邻国企业之间的小额贸易活动",'广西商务厅公开数据'),
('出口退税','Export Tax Rebate','Hoàn thuế xuất khẩu','คืนภาษีส่งออก','税务','',"对出口货物退还已征收的国内税款",'税务局'),
('跨境人民币结算','Cross-border RMB Settlement','Thanh toán RMB xuyên biên giới','การชำระเงินหยวนข้ามแดน','支付','',"使用人民币进行跨境贸易结算",'人民银行'),
('HS编码','HS Code','Mã HS','รหัส HS','报关','',"国际统一的商品分类编码体系",'海关总署'),
('提单','Bill of Lading','Vận đơn','ใบตราส่ง','物流','',"承运人签发的货物收据和运输合同证明",'交通运输部'),
('信用证','Letter of Credit','Thư tín dụng','เลตเตอร์ออฟเครดิต','支付','',"银行开立的有条件付款承诺",'人民银行'),
('反倾销税','Anti-dumping Duty','Thuế chống bán phá giá','ภาษีตอบโต้การทุ่มตลาด','税务','',"对倾销进口商品征收的附加税",'商务部'),
('自由贸易协定','Free Trade Agreement','Hiệp định thương mại tự do','ความตกลงการค้าเสรี','法律','',"两国或多国之间降低贸易壁垒的协议",'商务部'),
('检验检疫','Inspection and Quarantine','Kiểm dịch','การตรวจสอบและกักกัน','报关','',"对进出口货物进行质量和安全检验",'海关总署'),
('转口贸易','Re-export Trade','Thương nghiệp chuyển khẩu','การค้าเปลี่ยนถ่าย','报关','',"货物经第三国中转后再出口的贸易方式",'海关总署'),
('外汇核销','Foreign Exchange Verification','Quy đổi ngoại tệ','การตรวจสอบเงินตราต่างประเทศ','支付','',"对外汇收支进行核对和注销的手续",'外汇管理局'),
('仲裁条款','Arbitration Clause','Điều khoản trọng tài','ข้ออนุญาโตตุลาการ','法律','',"合同约定争议提交仲裁解决的条款",'广西法院'),
('保税仓库','Bonded Warehouse','Kho ngoại quan','คลังสินค้าทัณฑ์บน','物流','',"经海关批准存放保税货物的专用仓库",'海关总署');

-- ============================================
-- 纠纷案例库表
-- ============================================
CREATE TABLE IF NOT EXISTS t_dispute_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_no VARCHAR(50) COMMENT '案例编号',
    title VARCHAR(300) COMMENT '案例标题',
    case_type VARCHAR(50) COMMENT '类型：合同纠纷/知识产权/产品质量/支付纠纷',
    trade_category VARCHAR(100) COMMENT '贸易品类',
    countries VARCHAR(100) COMMENT '涉及国家',
    summary TEXT COMMENT '案例摘要',
    judgment TEXT COMMENT '判决结果',
    risk_advice TEXT COMMENT '风险建议',
    court VARCHAR(100) COMMENT '审理法院',
    publish_date VARCHAR(20) COMMENT '发布日期',
    source VARCHAR(200) COMMENT '数据来源',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='涉外商事纠纷案例库';

INSERT INTO t_dispute_case(case_no,title,case_type,trade_category,countries,summary,judgment,risk_advice,court,publish_date,source) VALUES
('GX-2025-001','越南买方拒付农产品货款案','合同纠纷','农产品','中国-越南','中方出口商向越南买方出口一批热带水果，货到后买方以质量不符为由拒付货款，涉及金额120万元。','法院判决买方支付80%货款，中方承担20%质量瑕疵责任。','签订合同时明确质量标准和检验方式，建议采用信用证结算。','南宁中院','2025-06-15','广西法院公开数据'),
('GX-2025-002','泰国转口贸易原产地证伪造案','合同纠纷','电子产品','中国-泰国-美国','中方企业通过泰国转口向美国出口电子产品，使用伪造的泰国原产地证享受优惠关税。','中方企业被处以货值3倍罚款，列入海关失信名单。','转口贸易务必确保原产地证真实合法，避免使用虚假中转文件。','南宁海关','2025-05-20','南宁海关公开数据'),
('GX-2025-003','马来西亚棕榈油质量纠纷案','产品质量','农产品','中国-马来西亚','中方进口马来西亚棕榈油，到货后发现酸价超标，不符合合同约定标准。','卖方退还50%货款并承担检验费用。','进口农产品应在合同中明确质量指标和第三方检验机构。','防城港法院','2025-04-10','广西法院公开数据'),
('GX-2025-004','印尼镍矿出口许可违规案','合同纠纷','矿产品','中国-印尼','中方企业在印尼投资镍矿项目，未取得当地出口许可证即擅自出口，被印尼海关扣留。','货物被没收，企业被列入印尼贸易黑名单。','投资印尼矿产资源前务必了解外资持股限制和出口许可要求。','最高法','2025-03-25','最高人民法院公开数据'),
('GX-2025-005','柬埔寨服装出口EBA关税欺诈案','合同纠纷','纺织品','中国-柬埔寨-欧盟','中方通过柬埔寨转口服装到欧盟，虚报柬埔寨原产地以享受EBA零关税。','涉案企业被欧盟追缴关税并罚款200万欧元。','享受优惠关税必须确保符合原产地规则，不得虚假申报。','欧盟法院','2025-02-18','欧盟法院公开数据'),
('GX-2025-006','老挝矿产运输损毁赔偿案','合同纠纷','矿产品','中国-老挝','中方承运老挝锰矿至中国口岸，运输途中因包装不当导致矿石散落损毁。','承运方赔偿货值60%损失。','矿产运输应采用专业包装和保险，明确运输责任划分。','凭祥法院','2025-01-30','广西法院公开数据'),
('GX-2024-007','越南商标侵权纠纷案','知识产权','日用品','中国-越南','中方企业在越南注册的商标被当地企业仿冒使用，涉及日化产品。','越南方停止侵权并赔偿30万元。','出口东盟前应提前在目标国注册商标，做好知识产权保护。','越南河内法院','2024-12-15','越南法院公开数据'),
('GX-2024-008','泰国物流延误违约案','合同纠纷','机械设备','中国-泰国','中方出口机械设备到泰国，物流承运商延误交货导致买方生产线停工。','物流承运商赔偿买方停工损失50万元。','选择有资质的国际物流商，合同中明确交货时间和违约条款。','东兴法院','2024-11-20','广西法院公开数据'),
('GX-2024-009','跨境支付汇率损失纠纷案','支付纠纷','农产品','中国-越南','中越贸易采用美元结算，汇率大幅波动导致一方遭受重大汇兑损失。','法院认定汇率风险由双方共同承担，各担50%。','跨境贸易建议在合同中约定汇率波动分担机制或使用人民币结算。','南宁中院','2024-10-05','广西法院公开数据'),
('GX-2024-010','印尼外资持股违规投资案','合同纠纷','化工产品','中国-印尼','中方企业在印尼投资化工项目，持股比例超过印尼外资限制上限。','投资项目被要求限期减持至合规比例。','投资印尼前须确认目标行业的外资持股上限，避免违规投资。','印尼投资协调委员会','2024-09-18','印尼政府公开数据');

-- ============================================
-- 比赛演示预警数据（明确标注为项目演示样例）
-- 完整、可重复执行版本见 sql/seed_warning_demo.sql
-- ============================================
INSERT INTO t_warning_message
  (credit_code, enterprise_name, warning_type, warning_level, warning_content,
   data_source, is_read, status, create_time)
SELECT '91450681MA5PPA1D5N', '广西东兴淘一二三进出口贸易有限公司', '低值高报', '中风险',
       '申报单价偏离同品类市场均价1.8个标准差，建议核验合同、发票、装箱单、报关单与价格依据。',
       '项目演示样例', 0, '未处理', DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM t_warning_message
  WHERE credit_code = '91450681MA5PPA1D5N' AND warning_type = '低值高报'
    AND warning_content = '申报单价偏离同品类市场均价1.8个标准差，建议核验合同、发票、装箱单、报关单与价格依据。'
);

-- ============================================
-- 风控阈值配置（管理端保存后由评分引擎实时读取）
-- ============================================
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

INSERT INTO t_warning_message
  (credit_code, enterprise_name, warning_type, warning_level, warning_content,
   data_source, is_read, status, create_time)
SELECT '91450681MA5N37BC0U', '东兴市亚宏进出口贸易有限公司', '票据异常', '中风险',
       '演示规则发现部分贸易单证字段不一致，建议核验发票、装箱单与报关单。',
       '项目演示样例', 0, '处理中', DATE_SUB(NOW(), INTERVAL 8 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM t_warning_message
  WHERE credit_code = '91450681MA5N37BC0U' AND warning_type = '票据异常'
    AND warning_content = '演示规则发现部分贸易单证字段不一致，建议核验发票、装箱单与报关单。'
);

INSERT INTO t_warning_message
  (credit_code, enterprise_name, warning_type, warning_level, warning_content,
   data_source, is_read, status, create_time)
SELECT '91450681MA5QCDGM2X', '东兴市众达贸易有限公司', '关联风险', '高风险',
       '跨境关联网络出现新增高风险节点，建议核验实际控制关系与交易背景。',
       '项目演示样例', 0, '未处理', DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM t_warning_message
  WHERE credit_code = '91450681MA5QCDGM2X' AND warning_type = '关联风险'
    AND warning_content = '跨境关联网络出现新增高风险节点，建议核验实际控制关系与交易背景。'
);

INSERT INTO t_warning_message
  (credit_code, enterprise_name, warning_type, warning_level, warning_content,
   data_source, is_read, status, create_time)
SELECT '91450681315843133T', '东兴市中越泰国际食品有限公司', '税务风险', '中风险',
       '出口退税申报节奏与历史区间存在偏离，建议开展凭证完整性复核。',
       '项目演示样例', 1, '已处理', DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM t_warning_message
  WHERE credit_code = '91450681315843133T' AND warning_type = '税务风险'
    AND warning_content = '出口退税申报节奏与历史区间存在偏离，建议开展凭证完整性复核。'
);

INSERT INTO t_warning_message
  (credit_code, enterprise_name, warning_type, warning_level, warning_content,
   data_source, is_read, status, create_time)
SELECT '91450681MAA79FX799', '东兴市安保达国际物流有限公司', '合规风险', '低风险',
       '目标国合规清单近期更新，建议在出运前重新核验许可与申报要求。',
       '项目演示样例', 1, '已关闭', DATE_SUB(NOW(), INTERVAL 5 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM t_warning_message
  WHERE credit_code = '91450681MAA79FX799' AND warning_type = '合规风险'
    AND warning_content = '目标国合规清单近期更新，建议在出运前重新核验许可与申报要求。'
);

INSERT INTO t_warning_message
  (credit_code, enterprise_name, warning_type, warning_level, warning_content,
   data_source, is_read, status, create_time)
SELECT '91450681MA5KL40R01', '柳州市振宇金属贸易有限公司东兴市分公司', '物流异常', '低风险',
       '口岸环节时效高于演示基线，建议关注车辆调度与查验资料准备情况。',
       '项目演示样例', 0, '未处理', DATE_SUB(NOW(), INTERVAL 8 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM t_warning_message
  WHERE credit_code = '91450681MA5KL40R01' AND warning_type = '物流异常'
    AND warning_content = '口岸环节时效高于演示基线，建议关注车辆调度与查验资料准备情况。'
);

-- ============================================
-- 样例数据统一标识：避免将比赛演示记录误作真实监管事实
-- ============================================
UPDATE t_abnormal_operation SET data_source = '项目演示样例（拟接入市场监管公开数据）';
UPDATE t_administrative_penalty SET data_source = '项目演示样例（拟接入市场监管公开数据）';
UPDATE t_customs_declaration SET data_source = '项目演示样例（拟接入口岸通关公开统计）';
UPDATE t_foreign_trade_record SET data_source = '项目演示样例（拟接入商务公开数据）';
UPDATE t_port_geo SET data_source = '项目演示样例（地理字段参考公开信息）';
UPDATE t_settlement_credit SET data_source = '项目演示样例（拟接入跨境结算公开统计）';
UPDATE t_fraud_alert SET data_source = '项目演示模型输出';
UPDATE t_trade_terminology SET source = '项目演示词库（术语字段参考公开规范）';
UPDATE t_dispute_case SET source = '项目演示案例（结构参考公开裁判信息）';
UPDATE t_policy SET data_source = '项目演示样例';
UPDATE t_warning_message SET data_source = '项目演示样例';

-- ============================================
-- 可视化查询：我的看板配置
-- ============================================
CREATE TABLE IF NOT EXISTS t_query_dashboard (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL DEFAULT 'admin',
    name        VARCHAR(128) NOT NULL,
    config_json TEXT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_dashboard_user (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
