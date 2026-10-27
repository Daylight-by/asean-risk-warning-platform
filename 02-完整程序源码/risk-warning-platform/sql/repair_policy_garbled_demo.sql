-- 修复当前比赛演示库中因错误客户端编码写入的 7 条政策样例。
-- 仅更新标题仍包含问号的指定演示记录，正常政策数据不会被覆盖。
SET NAMES utf8mb4;
USE risk_warning;

START TRANSACTION;

UPDATE t_policy
SET policy_title = '中国-东盟自贸区关税减让与原产地规则说明',
    policy_category = '关税',
    target_country = '东盟多国',
    publish_date = '2025-01-01',
    policy_content = '用于演示目标国家关税与原产地规则查询的政策摘要。',
    data_source = '项目演示样例',
    is_active = 1
WHERE id = 1 AND policy_title LIKE '%?%';

UPDATE t_policy
SET policy_title = '越南水果进口检验检疫要求',
    policy_category = '准入',
    target_country = '越南',
    publish_date = '2025-03-15',
    policy_content = '用于演示越南农产品进口资质、原产地证明和检疫材料提示。',
    data_source = '项目演示样例',
    is_active = 1
WHERE id = 2 AND policy_title LIKE '%?%';

UPDATE t_policy
SET policy_title = '凭祥口岸通关便利化措施',
    policy_category = '流程',
    target_country = '越南',
    publish_date = '2024-12-01',
    policy_content = '用于演示口岸提前申报、两步申报和鲜活农产品通关提示。',
    data_source = '项目演示样例',
    is_active = 1
WHERE id = 3 AND policy_title LIKE '%?%';

UPDATE t_policy
SET policy_title = '边境小额贸易税收优惠政策摘要',
    policy_category = '优惠',
    target_country = '东盟多国',
    publish_date = '2025-02-28',
    policy_content = '用于演示边境小额贸易优惠政策与适用条件提示。',
    data_source = '项目演示样例',
    is_active = 1
WHERE id = 4 AND policy_title LIKE '%?%';

UPDATE t_policy
SET policy_title = '东兴国家重点开发开放试验区政策摘要',
    policy_category = '优惠',
    target_country = '越南',
    publish_date = '2024-06-30',
    policy_content = '用于演示广西沿边开放平台政策检索与企业合规提醒。',
    data_source = '项目演示样例',
    is_active = 1
WHERE id = 5 AND policy_title LIKE '%?%';

UPDATE t_policy
SET policy_title = '中国-东盟跨境电商商品准入提示',
    policy_category = '准入',
    target_country = '东盟多国',
    publish_date = '2025-01-20',
    policy_content = '用于演示跨境电商商品准入、交易限值和材料准备提醒。',
    data_source = '项目演示样例',
    is_active = 1
WHERE id = 6 AND policy_title LIKE '%?%';

UPDATE t_policy
SET policy_title = '南宁海关通关一体化改革摘要',
    policy_category = '流程',
    target_country = '东盟多国',
    publish_date = '2024-09-15',
    policy_content = '用于演示通关一体化、无纸化申报和申报口岸选择提示。',
    data_source = '项目演示样例',
    is_active = 1
WHERE id = 7 AND policy_title LIKE '%?%';

COMMIT;
