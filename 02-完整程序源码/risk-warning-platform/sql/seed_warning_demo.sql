-- 比赛演示预警数据：仅补齐空白演示场景，不冒充真实监管记录。
-- 可重复执行；以企业信用代码 + 预警类型 + 内容作为幂等判断。
SET NAMES utf8mb4;
START TRANSACTION;

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

COMMIT;
