-- 东盟十国政策覆盖补齐：公开门户快照（演示用，逐条核验前不得作为法律意见）
INSERT INTO t_policy (policy_title, policy_category, target_country, publish_date, policy_content, data_source, link, is_active)
SELECT seed.policy_title, seed.policy_category, seed.target_country, seed.publish_date, seed.policy_content, seed.data_source, seed.link, 1
FROM (
  SELECT '泰国进口申报与原产地文件指引（公开门户快照）' policy_title, '流程' policy_category, '泰国' target_country, '2025-01-01' publish_date, '面向跨境贸易企业的申报、原产地文件与受控货物核验提示。' policy_content, '泰国海关公开门户快照·待逐条核验' data_source, 'https://www.customs.go.th/' link
  UNION ALL SELECT '马来西亚进出口许可与清真合规提示（公开门户快照）', '准入', '马来西亚', '2025-01-01', '面向食品、农产品等品类的许可、标签与清真合规核验提示。', '马来西亚皇家关税局公开门户快照·待逐条核验', 'https://www.customs.gov.my/en'
  UNION ALL SELECT '印度尼西亚进口许可与商品合规提示（公开门户快照）', '准入', '印度尼西亚', '2025-01-01', '面向受限商品进口许可、海关申报和贸易合规的核验提示。', '印尼海关公开门户快照·待逐条核验', 'https://www.beacukai.go.id/'
  UNION ALL SELECT '菲律宾跨境贸易申报与税费提示（公开门户快照）', '关税', '菲律宾', '2025-01-01', '面向进口申报、税费和受监管货物的公开指引检索提示。', '菲律宾海关公开门户快照·待逐条核验', 'https://customs.gov.ph/'
  UNION ALL SELECT '新加坡电子通关与贸易合规提示（公开门户快照）', '流程', '新加坡', '2025-01-01', '面向电子申报、贸易文件和受控货物合规的公开指引检索提示。', '新加坡海关公开门户快照·待逐条核验', 'https://www.customs.gov.sg/'
) seed
WHERE NOT EXISTS (SELECT 1 FROM t_policy p WHERE p.policy_title = seed.policy_title);
