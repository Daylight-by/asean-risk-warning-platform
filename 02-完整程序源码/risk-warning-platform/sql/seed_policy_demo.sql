-- 项目演示政策数据初始化脚本。
-- 数据均为演示样例，正式使用前应替换为经过核验并保留来源链接的公开政策数据。
USE risk_warning;

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
