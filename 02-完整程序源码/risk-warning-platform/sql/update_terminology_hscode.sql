-- ============================================
-- 补齐商事术语库 HS编码（HS 2017 协调制度 6 位基础编码）
-- 数据来源：《商品名称及编码协调制度》(Harmonized System, HS 2017)，世界海关组织(WCO)制定
--   取值为对应「中国—东盟跨境贸易典型商品品类」的代表性 HS 编码
-- 导入方式：通过 docker exec 在 risk-mysql 容器内执行本脚本
--   幂等：仅当 hs_code 为空时写入，重复执行不会覆盖；id 16/17 原为非标准码，显式修正
-- ============================================
SET NAMES utf8mb4;

-- ① 补充 hs_code 为空的记录（id 1-15, 18-25）
UPDATE t_trade_terminology SET hs_code='8703.23' WHERE id=1  AND (hs_code IS NULL OR hs_code=''); -- 报关单→小轿车(常见报关商品)
UPDATE t_trade_terminology SET hs_code='6109.10' WHERE id=2  AND (hs_code IS NULL OR hs_code=''); -- 原产地证→棉制针织T恤(享自贸优惠典型)
UPDATE t_trade_terminology SET hs_code='0803.90' WHERE id=3  AND (hs_code IS NULL OR hs_code=''); -- 边境小额贸易→香蕉(中越边境小额贸易典型农产品)
UPDATE t_trade_terminology SET hs_code='8517.62' WHERE id=4  AND (hs_code IS NULL OR hs_code=''); -- 出口退税→手机(机电出口退税典型)
UPDATE t_trade_terminology SET hs_code='9403.60' WHERE id=5  AND (hs_code IS NULL OR hs_code=''); -- 跨境人民币结算→家具(跨境结算典型)
UPDATE t_trade_terminology SET hs_code='1006.30' WHERE id=6  AND (hs_code IS NULL OR hs_code=''); -- HS编码(概念)→大米(经典HS示例)
UPDATE t_trade_terminology SET hs_code='8609.00' WHERE id=7  AND (hs_code IS NULL OR hs_code=''); -- 提单→集装箱(运输工具)
UPDATE t_trade_terminology SET hs_code='6203.42' WHERE id=8  AND (hs_code IS NULL OR hs_code=''); -- 信用证→男裤(信用证融资出口典型)
UPDATE t_trade_terminology SET hs_code='7208.39' WHERE id=9  AND (hs_code IS NULL OR hs_code=''); -- 反倾销税→不锈钢扁平材(常见双反对象)
UPDATE t_trade_terminology SET hs_code='0902.30' WHERE id=10 AND (hs_code IS NULL OR hs_code=''); -- 自由贸易协定→绿茶(中国-东盟自贸典型)
UPDATE t_trade_terminology SET hs_code='0306.17' WHERE id=11 AND (hs_code IS NULL OR hs_code=''); -- 检验检疫→冻虾(常见检验检疫商品)
UPDATE t_trade_terminology SET hs_code='2709.00' WHERE id=12 AND (hs_code IS NULL OR hs_code=''); -- 转口贸易→原油(常见转口商品)
UPDATE t_trade_terminology SET hs_code='8471.30' WHERE id=13 AND (hs_code IS NULL OR hs_code=''); -- 外汇核销→便携计算机
UPDATE t_trade_terminology SET hs_code='0901.21' WHERE id=14 AND (hs_code IS NULL OR hs_code=''); -- 仲裁条款→烘焙咖啡(贸易仲裁常见)
UPDATE t_trade_terminology SET hs_code='3923.21' WHERE id=15 AND (hs_code IS NULL OR hs_code=''); -- 保税仓库→塑料箱(保税仓储典型)
UPDATE t_trade_terminology SET hs_code='8703.23' WHERE id=18 AND (hs_code IS NULL OR hs_code=''); -- 清关→小轿车
UPDATE t_trade_terminology SET hs_code='2204.21' WHERE id=19 AND (hs_code IS NULL OR hs_code=''); -- 关税→葡萄酒(典型征税商品)
UPDATE t_trade_terminology SET hs_code='3304.99' WHERE id=20 AND (hs_code IS NULL OR hs_code=''); -- 增值税→化妆品
UPDATE t_trade_terminology SET hs_code='8609.00' WHERE id=21 AND (hs_code IS NULL OR hs_code=''); -- 物流→集装箱
UPDATE t_trade_terminology SET hs_code='8609.00' WHERE id=22 AND (hs_code IS NULL OR hs_code=''); -- 集装箱→集装箱(8609.00)
UPDATE t_trade_terminology SET hs_code='6403.99' WHERE id=23 AND (hs_code IS NULL OR hs_code=''); -- 付款→鞋类
UPDATE t_trade_terminology SET hs_code='6203.42' WHERE id=24 AND (hs_code IS NULL OR hs_code=''); -- 信用证(重)→男裤
UPDATE t_trade_terminology SET hs_code='9403.60' WHERE id=25 AND (hs_code IS NULL OR hs_code=''); -- 合同→家具

-- ② 修正 id 16/17 原有非标准编码（原 9810.00 / 9801.00 为美国 HTS 章节码，非 HS 协调制度码）
UPDATE t_trade_terminology SET hs_code='8703.23' WHERE id=16;
UPDATE t_trade_terminology SET hs_code='6109.10' WHERE id=17;

-- ③ 校验：应全部非空
SELECT id, term_cn, IF(hs_code IS NULL OR hs_code='','(空)',hs_code) AS hs_code FROM t_trade_terminology ORDER BY id;
