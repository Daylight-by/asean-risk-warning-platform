const i18n = require('./i18n');

const titleMap = {
  '泰国2025年跨境电商税收新规': { en: 'Thailand 2025 cross-border e-commerce tax rules', vi: 'Quy định thuế thương mại điện tử xuyên biên giới Thái Lan 2025' },
  '越南-欧盟自贸协定原产地规则更新': { en: 'Vietnam–EU FTA rules-of-origin update', vi: 'Cập nhật quy tắc xuất xứ FTA Việt Nam–EU' },
  '马来西亚2025年数字经济发展规划': { en: 'Malaysia 2025 digital economy development plan', vi: 'Kế hoạch phát triển kinh tế số Malaysia 2025' },
  '印尼食品安全进口新标准发布': { en: 'Indonesia issues new food-import safety standards', vi: 'Indonesia ban hành tiêu chuẩn mới về an toàn nhập khẩu thực phẩm' },
  '新加坡海关电子化通关全面升级': { en: 'Singapore Customs upgrades electronic clearance', vi: 'Hải quan Singapore nâng cấp thông quan điện tử' },
  '菲律宾跨境电商平台运营规范出台': { en: 'Philippines issues cross-border e-commerce platform rules', vi: 'Philippines ban hành quy định vận hành nền tảng thương mại điện tử xuyên biên giới' },
  '泰国投资促进委员会2025年投资优惠政策': { en: 'Thailand BOI 2025 investment incentives', vi: 'Ưu đãi đầu tư BOI Thái Lan năm 2025' },
  '越南2025年最低工资标准调整通知': { en: 'Vietnam 2025 minimum-wage adjustment notice', vi: 'Thông báo điều chỉnh lương tối thiểu Việt Nam 2025' },
  '越南买方拒付农产品货款案': { en: 'Vietnam buyer refuses payment for agricultural goods', vi: 'Người mua Việt Nam từ chối thanh toán hàng nông sản' },
  '泰国转口贸易原产地证伪造案': { en: 'Forged certificate of origin in Thailand transshipment trade', vi: 'Giả mạo chứng nhận xuất xứ trong thương mại chuyển khẩu qua Thái Lan' },
  '马来西亚棕榈油质量纠纷案': { en: 'Malaysia palm-oil quality dispute', vi: 'Tranh chấp chất lượng dầu cọ Malaysia' },
  '印尼镍矿出口许可违规案': { en: 'Indonesia nickel-export permit violation', vi: 'Vi phạm giấy phép xuất khẩu quặng niken Indonesia' },
  '越南商标侵权纠纷案': { en: 'Vietnam trademark-infringement dispute', vi: 'Tranh chấp xâm phạm nhãn hiệu tại Việt Nam' },
  '跨境支付汇率损失纠纷案': { en: 'Cross-border payment exchange-rate loss dispute', vi: 'Tranh chấp tổn thất tỷ giá trong thanh toán xuyên biên giới' }
};

const summaryPrefix = {
  en: 'AI-assisted demo translation. This record concerns cross-border trade compliance and risk; open the original text for the full source wording.',
  vi: 'Bản dịch minh họa có hỗ trợ AI. Bản ghi liên quan đến tuân thủ và rủi ro thương mại xuyên biên giới; hãy mở nguyên văn để xem đầy đủ nội dung nguồn.',
  zh: ''
};
const enterpriseAliases = {
  '广西东兴淘一二三进出口贸易有限公司': { en: 'Guangxi Dongxing Taoyiersan Import & Export Trading Co., Ltd.', vi: 'Công ty TNHH Thương mại Xuất nhập khẩu Taoyiersan Đông Hưng Quảng Tây' },
  '东兴市众达贸易有限公司': { en: 'Dongxing Zhongda Trading Co., Ltd.', vi: 'Công ty Thương mại Zhongda Đông Hưng' },
  '柳州市振宇金属贸易有限公司东兴市分公司': { en: 'Liuzhou Zhenyu Metal Trading Co., Ltd. Dongxing Branch', vi: 'Chi nhánh Đông Hưng Công ty Thương mại Kim loại Zhenyu Liễu Châu' },
  '东兴市亚宏进出口贸易有限公司': { en: 'Dongxing Yahong Import & Export Trading Co., Ltd.', vi: 'Công ty TNHH Xuất nhập khẩu Yahong Đông Hưng' },
  '东兴市中越泰国际食品有限公司': { en: 'Dongxing China–Vietnam–Thailand International Food Co., Ltd.', vi: 'Công ty Thực phẩm Quốc tế Trung–Việt–Thái Đông Hưng' },
  '东兴市安保达国际物流有限公司': { en: 'Dongxing Anbaoda International Logistics Co., Ltd.', vi: 'Công ty Logistics Quốc tế Anbaoda Đông Hưng' }
};

const bodyMap = {
  '申报单价偏离同品类市场均价1.8个标准差，建议核验合同、发票、装箱单、报关单与价格依据。': { en: 'The declared unit price deviates 1.8 standard deviations from the category market average. Verify the contract, invoice, packing list, customs declaration and pricing evidence.', vi: 'Đơn giá khai báo lệch 1,8 độ lệch chuẩn so với mức trung bình thị trường của cùng nhóm hàng. Cần kiểm tra hợp đồng, hóa đơn, phiếu đóng gói, tờ khai hải quan và căn cứ định giá.' },
  '演示规则发现部分贸易单证字段不一致，建议核验发票、装箱单与报关单。': { en: 'Demo rules found inconsistent fields across trade documents. Verify invoices, packing lists and customs declarations.', vi: 'Quy tắc minh họa phát hiện trường dữ liệu không nhất quán giữa chứng từ thương mại. Hãy kiểm tra hóa đơn, phiếu đóng gói và tờ khai hải quan.' },
  '跨境关联网络出现新增高风险节点，建议核验实际控制关系与交易背景。': { en: 'A new high-risk node appeared in the cross-border relationship network. Verify beneficial control and transaction background.', vi: 'Mạng lưới liên kết xuyên biên giới xuất hiện nút rủi ro cao mới. Hãy xác minh quyền kiểm soát thực tế và bối cảnh giao dịch.' },
  '出口退税申报节奏与历史区间存在偏离，建议开展凭证完整性复核。': { en: 'Export-tax-refund filing patterns deviate from the historical range. Review supporting-document completeness.', vi: 'Nhịp độ khai hoàn thuế xuất khẩu lệch khỏi phạm vi lịch sử. Nên rà soát tính đầy đủ của chứng từ.' },
  '目标国合规清单近期更新，建议在出运前重新核验许可与申报要求。': { en: 'The destination-country compliance checklist was recently updated. Recheck permits and declaration requirements before shipment.', vi: 'Danh mục tuân thủ của nước đích vừa cập nhật. Hãy kiểm tra lại giấy phép và yêu cầu khai báo trước khi xuất hàng.' },
  '口岸环节时效高于演示基线，建议关注车辆调度与查验资料准备情况。': { en: 'Port processing time is above the demo baseline. Review vehicle scheduling and inspection-document readiness.', vi: 'Thời gian xử lý tại cửa khẩu cao hơn ngưỡng minh họa. Hãy theo dõi điều phối xe và chuẩn bị hồ sơ kiểm tra.' },
  '用于演示目标国家关税与原产地规则查询的政策摘要。': { en: 'Policy summary for demonstrating tariff and rules-of-origin queries by destination country.', vi: 'Tóm tắt chính sách minh họa việc tra cứu thuế quan và quy tắc xuất xứ theo nước đích.' },
  '用于演示越南农产品进口资质、原产地证明和检疫材料提示。': { en: 'Demo guidance on Vietnam agricultural-import qualifications, certificates of origin and quarantine documents.', vi: 'Hướng dẫn minh họa về điều kiện nhập nông sản vào Việt Nam, chứng nhận xuất xứ và hồ sơ kiểm dịch.' },
  '用于演示口岸提前申报、两步申报和鲜活农产品通关提示。': { en: 'Demo guidance on advance declaration, two-step declaration and clearance for perishable agricultural products.', vi: 'Hướng dẫn minh họa về khai báo trước, khai báo hai bước và thông quan nông sản tươi sống.' },
  '用于演示边境小额贸易优惠政策与适用条件提示。': { en: 'Demo summary of border small-trade incentives and applicability conditions.', vi: 'Tóm tắt minh họa về ưu đãi thương mại tiểu ngạch biên giới và điều kiện áp dụng.' },
  '用于演示广西沿边开放平台政策检索与企业合规提醒。': { en: 'Demo guidance for policy search and company-compliance reminders on Guangxi border-opening platforms.', vi: 'Hướng dẫn minh họa tra cứu chính sách và nhắc nhở tuân thủ doanh nghiệp tại các nền tảng mở cửa biên giới Quảng Tây.' },
  '用于演示跨境电商商品准入、交易限值和材料准备提醒。': { en: 'Demo guidance on cross-border e-commerce product access, transaction limits and document preparation.', vi: 'Hướng dẫn minh họa về điều kiện tiếp cận hàng hóa thương mại điện tử xuyên biên giới, giới hạn giao dịch và chuẩn bị hồ sơ.' },
  '用于演示通关一体化、无纸化申报和申报口岸选择提示。': { en: 'Demo guidance on integrated clearance, paperless declaration and port-of-declaration selection.', vi: 'Hướng dẫn minh họa về thông quan tích hợp, khai báo không giấy tờ và lựa chọn cửa khẩu khai báo.' },
  '文莱政府发布与中国的经贸合作规划，重点推动清真食品认证互认、油气下游产业合作及中小企业跨境撮合，简化双边贸易单证流程。': { en: 'Brunei–China trade-cooperation plan covering halal-certification recognition, downstream oil-and-gas cooperation, SME matchmaking and simplified trade documents.', vi: 'Kế hoạch hợp tác thương mại Brunei–Trung Quốc về công nhận chứng nhận halal, hợp tác dầu khí hạ nguồn, kết nối doanh nghiệp nhỏ và đơn giản hóa chứng từ thương mại.' },
  '柬埔寨海关推行单一窗口（CamDigiKey）与电子报关，对东盟原产货物落实 ATIGA 零关税清单，降低中小商户合规成本。': { en: 'Cambodia Customs promotes the CamDigiKey single window and e-declaration, with ATIGA zero-tariff treatment for ASEAN-origin goods.', vi: 'Hải quan Campuchia thúc đẩy một cửa CamDigiKey và khai báo điện tử, áp dụng ưu đãi ATIGA cho hàng có xuất xứ ASEAN.' },
  '面向跨境贸易企业的申报、原产地文件与受控货物核验提示。': { en: 'Public-portal guidance on declarations, origin documents and controlled-goods verification for cross-border traders.', vi: 'Hướng dẫn từ cổng công khai về khai báo, chứng từ xuất xứ và xác minh hàng hóa có kiểm soát cho doanh nghiệp xuyên biên giới.' },
  '面向食品、农产品等品类的许可、标签与清真合规核验提示。': { en: 'Public-portal guidance on permits, labelling and halal-compliance checks for food and agricultural products.', vi: 'Hướng dẫn từ cổng công khai về giấy phép, nhãn hàng và kiểm tra tuân thủ halal cho thực phẩm và nông sản.' },
  '面向受限商品进口许可、海关申报和贸易合规的核验提示。': { en: 'Public-portal guidance on restricted-goods import permits, customs declarations and trade-compliance checks.', vi: 'Hướng dẫn từ cổng công khai về giấy phép nhập hàng hạn chế, khai báo hải quan và kiểm tra tuân thủ thương mại.' },
  '面向进口申报、税费和受监管货物的公开指引检索提示。': { en: 'Public-portal guidance on import declarations, duties and regulated goods.', vi: 'Hướng dẫn từ cổng công khai về khai báo nhập khẩu, thuế phí và hàng hóa quản lý chuyên ngành.' },
  '面向电子申报、贸易文件和受控货物合规的公开指引检索提示。': { en: 'Public-portal guidance on electronic declarations, trade documents and controlled-goods compliance.', vi: 'Hướng dẫn từ cổng công khai về khai báo điện tử, chứng từ thương mại và tuân thủ hàng hóa có kiểm soát.' }
};

function text(original, language) {
  if (language === 'zh') return original;
  // 未命中本地兜底词典时保留原文，避免把不同记录显示成同一段“假翻译”。
  // 联网场景会优先由 /api/i18n/content 返回可追溯的数据库译文。
  return (titleMap[original] && titleMap[original][language]) || (bodyMap[original] && bodyMap[original][language]) || original;
}
function decorate(item, fields) {
  const language = i18n.getLanguage();
  const result = { ...item, original: {}, translated: {}, isTranslated: language !== 'zh' };
  fields.forEach(field => {
    result.original[field] = item[field] || '';
    result.translated[field] = text(item[field] || '', language);
    result[`display${field.charAt(0).toUpperCase()}${field.slice(1)}`] = language === 'zh' ? (item[field] || '') : result.translated[field];
  });
  result.translationMeta = language === 'zh' ? '' : (language === 'en' ? 'AI-assisted translation · Demo · Source traceable' : 'Dịch có hỗ trợ AI · Minh họa · Có thể truy vết nguồn');
  return result;
}

function enterpriseAlias(name) {
  const language = i18n.getLanguage();
  return language === 'zh' ? name : ((enterpriseAliases[name] && enterpriseAliases[name][language]) || name);
}

module.exports = { decorate, enterpriseAlias };
