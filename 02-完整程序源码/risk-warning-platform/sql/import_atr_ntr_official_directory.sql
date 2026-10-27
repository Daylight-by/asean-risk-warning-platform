-- ASEAN Trade Repository verified National Trade Repository directory.
-- This UTF-8 file should be executed through MySQL SOURCE from an ASCII-only path.
SET NAMES utf8mb4;

INSERT INTO t_policy (policy_title, policy_category, target_country, publish_date, policy_content, data_source, link, is_active)
SELECT seed.policy_title, '流程', seed.target_country, NULL, seed.policy_content,
       'ASEAN Trade Repository（国家贸易资料库官方目录，已核验）', seed.link, 1
FROM (
  SELECT '文莱国家贸易资料库（NTR）官方入口' policy_title, '文莱' target_country, 'ASEAN Trade Repository 核验的文莱国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。' policy_content, 'https://www.bdntr.gov.bn' link
  UNION ALL SELECT '柬埔寨国家贸易资料库（NTR）官方入口', '柬埔寨', 'ASEAN Trade Repository 核验的柬埔寨国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://www.cambodiantr.gov.kh'
  UNION ALL SELECT '印度尼西亚国家贸易资料库（NTR）官方入口', '印度尼西亚', 'ASEAN Trade Repository 核验的印度尼西亚国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://www.insw.go.id/intr'
  UNION ALL SELECT '老挝国家贸易资料库（NTR）官方入口', '老挝', 'ASEAN Trade Repository 核验的老挝国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://www.laotradeportal.gov.la'
  UNION ALL SELECT '马来西亚国家贸易资料库（NTR）官方入口', '马来西亚', 'ASEAN Trade Repository 核验的马来西亚国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://mytraderepository.customs.gov.my'
  UNION ALL SELECT '缅甸国家贸易资料库（NTR）官方入口', '缅甸', 'ASEAN Trade Repository 核验的缅甸国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://www.myanmartradeportal.gov.mm'
  UNION ALL SELECT '菲律宾国家贸易资料库（NTR）官方入口', '菲律宾', 'ASEAN Trade Repository 核验的菲律宾国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://pntr.gov.ph'
  UNION ALL SELECT '新加坡国家贸易资料库（NTR）官方入口', '新加坡', 'ASEAN Trade Repository 核验的新加坡国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://www.fta.gov.sg'
  UNION ALL SELECT '泰国国家贸易资料库（NTR）官方入口', '泰国', 'ASEAN Trade Repository 核验的泰国国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://www.thailandntr.com'
  UNION ALL SELECT '越南国家贸易资料库（NTR）官方入口', '越南', 'ASEAN Trade Repository 核验的越南国家贸易资料库入口，提供关税、优惠关税、原产地规则、非关税措施、通关程序与单证要求等查询。该记录为官方导航，不替代具体法规正文。', 'https://ntr.moit.gov.vn'
) seed
WHERE NOT EXISTS (SELECT 1 FROM t_policy p WHERE p.policy_title = seed.policy_title);

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'title', 'zh', policy_title, 'en', CONCAT(CASE target_country
  WHEN '文莱' THEN 'Brunei Darussalam' WHEN '柬埔寨' THEN 'Cambodia' WHEN '印度尼西亚' THEN 'Indonesia'
  WHEN '老挝' THEN 'Lao PDR' WHEN '马来西亚' THEN 'Malaysia' WHEN '缅甸' THEN 'Myanmar'
  WHEN '菲律宾' THEN 'Philippines' WHEN '新加坡' THEN 'Singapore' WHEN '泰国' THEN 'Thailand'
  WHEN '越南' THEN 'Viet Nam' ELSE target_country END,
  ' National Trade Repository (NTR) — official portal'), 'human_curated', 'source_checked'
FROM t_policy WHERE policy_title LIKE '%国家贸易资料库（NTR）官方入口'
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translation_status=VALUES(translation_status), verification_status=VALUES(verification_status), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'summary', 'zh', policy_content, 'en', 'Official NTR portal verified through the ASEAN Trade Repository. It provides access to tariff, preferential-tariff, rules-of-origin, non-tariff-measure, procedure and documentary-requirement information. This is an official navigation record, not legal advice.', 'human_curated', 'source_checked'
FROM t_policy WHERE policy_title LIKE '%国家贸易资料库（NTR）官方入口'
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translation_status=VALUES(translation_status), verification_status=VALUES(verification_status), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'title', 'zh', policy_title, 'vi', 'Cổng Thông tin Thương mại Quốc gia (NTR) — nguồn chính thức', 'human_curated', 'source_checked'
FROM t_policy WHERE policy_title LIKE '%国家贸易资料库（NTR）官方入口'
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translation_status=VALUES(translation_status), verification_status=VALUES(verification_status), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'summary', 'zh', policy_content, 'vi', 'Cổng NTR chính thức được xác minh qua ASEAN Trade Repository. Cổng cung cấp thông tin về thuế quan, thuế ưu đãi, quy tắc xuất xứ, biện pháp phi thuế quan, thủ tục và yêu cầu chứng từ. Đây là bản ghi điều hướng chính thức, không thay thế tư vấn pháp lý.', 'human_curated', 'source_checked'
FROM t_policy WHERE policy_title LIKE '%国家贸易资料库（NTR）官方入口'
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translation_status=VALUES(translation_status), verification_status=VALUES(verification_status), translated_at=CURRENT_TIMESTAMP;
