-- 将现有比赛库中的样例记录统一标识为演示数据，避免误作真实监管结论。
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE t_abnormal_operation
SET data_source = '项目演示样例（拟接入市场监管公开数据）';

UPDATE t_administrative_penalty
SET data_source = '项目演示样例（拟接入市场监管公开数据）';

UPDATE t_customs_declaration
SET data_source = '项目演示样例（拟接入口岸通关公开统计）';

UPDATE t_foreign_trade_record
SET data_source = '项目演示样例（拟接入商务公开数据）';

UPDATE t_port_geo
SET data_source = '项目演示样例（地理字段参考公开信息）';

UPDATE t_settlement_credit
SET data_source = '项目演示样例（拟接入跨境结算公开统计）';

UPDATE t_fraud_alert
SET data_source = '项目演示模型输出';

UPDATE t_trade_terminology
SET source = '项目演示词库（术语字段参考公开规范）';

UPDATE t_dispute_case
SET source = '项目演示案例（结构参考公开裁判信息）';

UPDATE t_policy
SET data_source = '项目演示样例';

UPDATE t_warning_message
SET data_source = '项目演示样例';

COMMIT;
