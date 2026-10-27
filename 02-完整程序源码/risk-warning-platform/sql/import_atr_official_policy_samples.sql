-- Verified policy samples from ASEAN Trade Repository (ATR).
-- This file is UTF-8. Run through MySQL's SOURCE command after extracting to an ASCII-only path.
SET NAMES utf8mb4;

INSERT INTO t_policy (policy_title, policy_category, target_country, publish_date, policy_content, data_source, link, is_active)
SELECT 'Wholesome Meat (Import, Export and Transhipment) Regulations 2011', '准入', '文莱', '2011-02-17',
       '官方 ATR 条目显示：进口冷冻清真肉须取得进口许可；应以原始法规及主管部门最新要求为准。该记录保留英文原始法规名称与官方链接。',
       'ASEAN Trade Repository（官方来源，已核验）',
       'https://atr.asean.org/links/result?country_code=bn&id_topic1=44&id_topic2=62&id_topic3=&key=&level=3', 1
WHERE NOT EXISTS (SELECT 1 FROM t_policy WHERE policy_title = 'Wholesome Meat (Import, Export and Transhipment) Regulations 2011');

INSERT INTO t_policy (policy_title, policy_category, target_country, publish_date, policy_content, data_source, link, is_active)
SELECT 'Export/Import Bulletin (4/2024) — non-automatic import licence', '准入', '缅甸', '2024-08-01',
       '官方 ATR 条目显示：该公告对 1,460 个 HS 10 位税目实施非自动进口许可；具体税目和有效性应以缅甸国家贸易门户最新公告为准。',
       'ASEAN Trade Repository（官方来源，已核验）',
       'https://atr.asean.org/links/result?country_code=mm&id_topic1=44&key=&level=2', 1
WHERE NOT EXISTS (SELECT 1 FROM t_policy WHERE policy_title = 'Export/Import Bulletin (4/2024) — non-automatic import licence');

INSERT INTO t_policy (policy_title, policy_category, target_country, publish_date, policy_content, data_source, link, is_active)
SELECT 'Singapore import labelling requirements for meat and fish products', '准入', '新加坡', NULL,
       '官方 ATR 条目列出：进口肉类或鱼类产品标签应包含产品描述、原产国、品牌及加工、包装、批次、净重等信息；以主管部门最新规定为准。',
       'ASEAN Trade Repository（官方来源，已核验）',
       'https://atr.asean.org/links/result?country_code=sg&id_topic1=44&id_topic2=62&id_topic3=&key=&level=3', 1
WHERE NOT EXISTS (SELECT 1 FROM t_policy WHERE policy_title = 'Singapore import labelling requirements for meat and fish products');
