-- 首批可追溯英越译文：政策标题/摘要、预警摘要、企业别名
INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'warning', CAST(id AS CHAR), 'content', 'zh', warning_content, 'en',
CASE id WHEN 1 THEN 'Declared cargo value changed significantly in the last 30 days. Reconcile orders, contracts and payment flows.' WHEN 2 THEN 'Demo rules found inconsistent fields across trade documents. Verify invoices, packing lists and customs declarations.' WHEN 3 THEN 'A new high-risk node appeared in the cross-border relationship network. Verify beneficial control and transaction background.' WHEN 4 THEN 'Export-tax-refund filing patterns deviate from the historical range. Review supporting-document completeness.' WHEN 5 THEN 'The destination-country compliance checklist was recently updated. Recheck permits and declaration requirements before shipment.' ELSE 'Port processing time is above the demo baseline. Review vehicle scheduling and inspection-document readiness.' END,
'demo_ai_assisted','pending_verification' FROM t_warning_message
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'warning', CAST(id AS CHAR), 'content', 'zh', warning_content, 'vi',
CASE id WHEN 1 THEN 'Giá trị hàng khai báo biến động đáng kể trong 30 ngày qua. Cần đối chiếu đơn hàng, hợp đồng và dòng tiền.' WHEN 2 THEN 'Quy tắc minh họa phát hiện trường dữ liệu không nhất quán giữa chứng từ thương mại. Hãy kiểm tra hóa đơn, phiếu đóng gói và tờ khai hải quan.' WHEN 3 THEN 'Mạng lưới liên kết xuyên biên giới xuất hiện nút rủi ro cao mới. Hãy xác minh quyền kiểm soát thực tế và bối cảnh giao dịch.' WHEN 4 THEN 'Nhịp độ khai hoàn thuế xuất khẩu lệch khỏi phạm vi lịch sử. Nên rà soát tính đầy đủ của chứng từ.' WHEN 5 THEN 'Danh mục tuân thủ của nước đích vừa cập nhật. Hãy kiểm tra lại giấy phép và yêu cầu khai báo trước khi xuất hàng.' ELSE 'Thời gian xử lý tại cửa khẩu cao hơn ngưỡng minh họa. Hãy theo dõi điều phối xe và chuẩn bị hồ sơ kiểm tra.' END,
'demo_ai_assisted','pending_verification' FROM t_warning_message
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'title', 'zh', policy_title, 'vi',
CASE id WHEN 1 THEN 'Ưu đãi thuế quan và quy tắc xuất xứ Trung Quốc–ASEAN' WHEN 2 THEN 'Yêu cầu kiểm dịch và kiểm tra nhập khẩu trái cây vào Việt Nam' WHEN 3 THEN 'Biện pháp tạo thuận lợi thông quan tại cửa khẩu Pingxiang' WHEN 4 THEN 'Ưu đãi thuế cho thương mại tiểu ngạch biên giới' WHEN 5 THEN 'Tóm tắt chính sách khu thí điểm mở cửa Dongxing' WHEN 6 THEN 'Hướng dẫn tiếp cận thị trường thương mại điện tử xuyên biên giới Trung Quốc–ASEAN' WHEN 7 THEN 'Tóm tắt cải cách thông quan tích hợp của Hải quan Nam Ninh' WHEN 8 THEN 'Kế hoạch hợp tác thương mại Brunei–Trung Quốc' WHEN 9 THEN 'Biện pháp tạo thuận lợi hải quan và thuế xuất nhập khẩu Campuchia' WHEN 10 THEN 'Ưu đãi khu hợp tác kinh tế Lào–Trung Quốc' WHEN 11 THEN 'Quy định quản lý thương mại biên giới Myanmar (sửa đổi)' WHEN 12 THEN 'Hướng dẫn khai báo nhập khẩu và chứng từ xuất xứ Thái Lan' WHEN 13 THEN 'Hướng dẫn giấy phép xuất nhập khẩu và tuân thủ halal Malaysia' WHEN 14 THEN 'Hướng dẫn giấy phép nhập khẩu và tuân thủ sản phẩm Indonesia' WHEN 15 THEN 'Hướng dẫn khai báo xuyên biên giới và thuế phí Philippines' ELSE 'Hướng dẫn thông quan điện tử và tuân thủ thương mại Singapore' END,
'demo_ai_assisted','pending_verification' FROM t_policy
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'enterprise', enterprise_name, 'alias', 'zh', enterprise_name, 'en',
CASE enterprise_name WHEN '广西东兴淘一二三进出口贸易有限公司' THEN 'Guangxi Dongxing Taoyiersan Import & Export Trading Co., Ltd.' WHEN '东兴市亚宏进出口贸易有限公司' THEN 'Dongxing Yahong Import & Export Trading Co., Ltd.' WHEN '东兴市众达贸易有限公司' THEN 'Dongxing Zhongda Trading Co., Ltd.' WHEN '东兴市中越泰国际食品有限公司' THEN 'Dongxing China–Vietnam–Thailand International Food Co., Ltd.' WHEN '东兴市安保达国际物流有限公司' THEN 'Dongxing Anbaoda International Logistics Co., Ltd.' ELSE 'Liuzhou Zhenyu Metal Trading Co., Ltd. Dongxing Branch' END,
'demo_ai_assisted','pending_verification' FROM t_warning_message
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'title', 'zh', policy_title, 'en',
CASE id WHEN 1 THEN 'China–ASEAN tariff concessions and rules of origin' WHEN 2 THEN 'Vietnam fruit import inspection and quarantine requirements' WHEN 3 THEN 'Pingxiang port clearance facilitation measures' WHEN 4 THEN 'Border small-trade tax incentives' WHEN 5 THEN 'Dongxing opening pilot-zone policy summary' WHEN 6 THEN 'China–ASEAN cross-border e-commerce market-access guidance' WHEN 7 THEN 'Nanning Customs integrated-clearance reform summary' WHEN 8 THEN 'Brunei–China trade cooperation plan' WHEN 9 THEN 'Cambodia import-export tax and customs facilitation measures' WHEN 10 THEN 'Lao PDR–China economic cooperation-zone incentives' WHEN 11 THEN 'Myanmar border-trade management rules (revision)' WHEN 12 THEN 'Thailand import declaration and origin-document guidance' WHEN 13 THEN 'Malaysia import-export licensing and halal compliance guidance' WHEN 14 THEN 'Indonesia import permits and product-compliance guidance' WHEN 15 THEN 'Philippines cross-border declaration and duty guidance' ELSE 'Singapore electronic clearance and trade-compliance guidance' END,
'demo_ai_assisted','pending_verification' FROM t_policy
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;

-- 政策正文也单独入库。页面展示优先取本表，不再使用通用的“演示翻译”占位文案。
INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'summary', 'zh', policy_content, 'en',
CASE id
  WHEN 1 THEN 'Policy summary for demonstrating tariff and rules-of-origin queries by destination country.'
  WHEN 2 THEN 'Demo guidance on Vietnam agricultural-import qualifications, certificates of origin and quarantine documents.'
  WHEN 3 THEN 'Demo guidance on advance declaration, two-step declaration and clearance for perishable agricultural products.'
  WHEN 4 THEN 'Demo summary of border small-trade incentives and applicability conditions.'
  WHEN 5 THEN 'Demo guidance for policy search and company-compliance reminders on Guangxi border-opening platforms.'
  WHEN 6 THEN 'Demo guidance on cross-border e-commerce product access, transaction limits and document preparation.'
  WHEN 7 THEN 'Demo guidance on integrated clearance, paperless declaration and port-of-declaration selection.'
  WHEN 8 THEN 'Brunei–China trade-cooperation plan covering halal-certification recognition, downstream oil-and-gas cooperation, SME matchmaking and simplified trade documents.'
  WHEN 9 THEN 'Cambodia Customs promotes the CamDigiKey single window and e-declaration, with ATIGA zero-tariff treatment for ASEAN-origin goods.'
  WHEN 10 THEN 'Public-portal guidance on declarations, certificates of origin, investment registration and border-trade compliance for Lao PDR–China trade.'
  WHEN 11 THEN 'Public-portal guidance on border-trade registration, import and export control, customs declarations and compliance checks in Myanmar.'
  WHEN 12 THEN 'Public-portal guidance on declarations, origin documents and controlled-goods verification for cross-border traders.'
  WHEN 13 THEN 'Public-portal guidance on permits, labelling and halal-compliance checks for food and agricultural products.'
  WHEN 14 THEN 'Public-portal guidance on restricted-goods import permits, customs declarations and trade-compliance checks.'
  WHEN 15 THEN 'Public-portal guidance on import declarations, duties and regulated goods.'
  ELSE 'Public-portal guidance on electronic declarations, trade documents and controlled-goods compliance.'
END,
'demo_ai_assisted','pending_verification' FROM t_policy
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'policy', CAST(id AS CHAR), 'summary', 'zh', policy_content, 'vi',
CASE id
  WHEN 1 THEN 'Tóm tắt chính sách minh họa việc tra cứu ưu đãi thuế quan và quy tắc xuất xứ theo nước đích.'
  WHEN 2 THEN 'Hướng dẫn minh họa về điều kiện nhập khẩu nông sản vào Việt Nam, chứng nhận xuất xứ và hồ sơ kiểm dịch.'
  WHEN 3 THEN 'Hướng dẫn minh họa về khai báo trước, khai báo hai bước và thông quan nông sản tươi sống.'
  WHEN 4 THEN 'Tóm tắt minh họa về ưu đãi thương mại tiểu ngạch biên giới và điều kiện áp dụng.'
  WHEN 5 THEN 'Hướng dẫn minh họa tra cứu chính sách và nhắc nhở tuân thủ doanh nghiệp tại các nền tảng mở cửa biên giới Quảng Tây.'
  WHEN 6 THEN 'Hướng dẫn minh họa về điều kiện tiếp cận hàng hóa thương mại điện tử xuyên biên giới, giới hạn giao dịch và chuẩn bị hồ sơ.'
  WHEN 7 THEN 'Hướng dẫn minh họa về thông quan tích hợp, khai báo không giấy tờ và lựa chọn cửa khẩu khai báo.'
  WHEN 8 THEN 'Kế hoạch hợp tác thương mại Brunei–Trung Quốc về công nhận chứng nhận halal, hợp tác dầu khí hạ nguồn, kết nối doanh nghiệp nhỏ và đơn giản hóa chứng từ thương mại.'
  WHEN 9 THEN 'Hải quan Campuchia thúc đẩy một cửa CamDigiKey và khai báo điện tử, áp dụng ưu đãi ATIGA cho hàng có xuất xứ ASEAN.'
  WHEN 10 THEN 'Hướng dẫn cổng công khai về khai báo, chứng nhận xuất xứ, đăng ký đầu tư và tuân thủ thương mại biên giới Lào–Trung Quốc.'
  WHEN 11 THEN 'Hướng dẫn cổng công khai về đăng ký thương mại biên giới, kiểm soát xuất nhập khẩu, khai báo hải quan và kiểm tra tuân thủ tại Myanmar.'
  WHEN 12 THEN 'Hướng dẫn cổng công khai về khai báo, chứng từ xuất xứ và xác minh hàng hóa có kiểm soát cho doanh nghiệp xuyên biên giới.'
  WHEN 13 THEN 'Hướng dẫn cổng công khai về giấy phép, nhãn hàng và kiểm tra tuân thủ halal cho thực phẩm và nông sản.'
  WHEN 14 THEN 'Hướng dẫn cổng công khai về giấy phép nhập hàng hạn chế, khai báo hải quan và kiểm tra tuân thủ thương mại.'
  WHEN 15 THEN 'Hướng dẫn cổng công khai về khai báo nhập khẩu, thuế phí và hàng hóa quản lý chuyên ngành.'
  ELSE 'Hướng dẫn cổng công khai về khai báo điện tử, chứng từ thương mại và tuân thủ hàng hóa có kiểm soát.'
END,
'demo_ai_assisted','pending_verification' FROM t_policy
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;

INSERT INTO t_multilingual_content (business_type,business_id,field_name,source_language,source_text,locale,translated_text,translation_status,verification_status)
SELECT 'enterprise', enterprise_name, 'alias', 'zh', enterprise_name, 'vi',
CASE enterprise_name
  WHEN '广西东兴淘一二三进出口贸易有限公司' THEN 'Công ty TNHH Thương mại Xuất nhập khẩu Taoyiersan Đông Hưng Quảng Tây'
  WHEN '东兴市亚宏进出口贸易有限公司' THEN 'Công ty TNHH Xuất nhập khẩu Yahong Đông Hưng'
  WHEN '东兴市众达贸易有限公司' THEN 'Công ty Thương mại Zhongda Đông Hưng'
  WHEN '东兴市中越泰国际食品有限公司' THEN 'Công ty Thực phẩm Quốc tế Trung–Việt–Thái Đông Hưng'
  WHEN '东兴市安保达国际物流有限公司' THEN 'Công ty Logistics Quốc tế Anbaoda Đông Hưng'
  ELSE 'Chi nhánh Đông Hưng Công ty Thương mại Kim loại Zhenyu Liễu Châu'
END,
'demo_ai_assisted','pending_verification' FROM t_warning_message
ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text), translated_at=CURRENT_TIMESTAMP;
