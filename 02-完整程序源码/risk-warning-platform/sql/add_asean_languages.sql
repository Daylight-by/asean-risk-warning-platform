-- ============================================
-- 术语库扩展东盟三语种（马来/印尼/柬埔寨）
-- 幂等：列已存在则跳过 ALTER
-- ============================================
SET @db = 'risk_warning', @tbl = 't_trade_terminology';
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = @tbl AND COLUMN_NAME = 'term_my');
SET @sql = IF(@col = 0,
    'ALTER TABLE t_trade_terminology ADD COLUMN term_my VARCHAR(200) DEFAULT NULL COMMENT ''马来语(Bahasa Melayu)'', ADD COLUMN term_in VARCHAR(200) DEFAULT NULL COMMENT ''印尼语(Bahasa Indonesia)'', ADD COLUMN term_km VARCHAR(200) DEFAULT NULL COMMENT ''柬埔寨语(Khmer)''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 仅当样例尚未插入时补充（按 source 标记去重）
SET @exists = (SELECT COUNT(*) FROM t_trade_terminology WHERE source = '广西公共数据开放平台·东盟术语库');
SET @ins = IF(@exists = 0, 'INSERT INTO t_trade_terminology
  (term_cn, term_en, term_vn, term_th, term_my, term_in, term_km, category, hs_code, description, source) VALUES
  (''报关'', ''Customs Declaration'', ''Khai báo hải quan'', ''ประกาศศุลกากร'', ''Kastam'', ''Pabean'', ''គយ'', ''报关'', ''9810.00'', ''向海关申报进出口货物的法定程序'', ''广西公共数据开放平台·东盟术语库''),
  (''原产地证'', ''Certificate of Origin'', ''Giấy chứng nhận xuất xứ'', ''ใบรับรองแหล่งกำเนิด'', ''Sijil Asal'', ''Sertifikat Asal'', ''វិក្កយបត្របង្កើត'', ''报关'', ''9801.00'', ''证明货物原产国的官方文件，关乎关税优惠'', ''广西公共数据开放平台·东盟术语库''),
  (''清关'', ''Customs Clearance'', ''Thông quan'', ''การเคลียร์ศุลกากร'', ''Pembersihan Kastam'', ''Pembersihan Bea Cukai'', ''ការជម្រះគយ'', ''报关'', NULL, ''货物通过海关查验、缴税后获准放行'', ''广西公共数据开放平台·东盟术语库''),
  (''关税'', ''Tariff'', ''Thuế quan'', ''ภาษีศุลกากร'', ''Tarif'', ''Tarif'', ''ពន្ធ'', ''税务'', NULL, ''进出口货物应缴纳的税种'', ''广西公共数据开放平台·东盟术语库''),
  (''增值税'', ''Value Added Tax'', ''Thuế GTGT'', ''ภาษีมูลค่าเพิ่ม'', ''GST'', ''PPN'', ''ពន្ធលើតម្លៃបន្ថែម'', ''税务'', NULL, ''货物增值环节征收的流转税'', ''广西公共数据开放平台·东盟术语库''),
  (''物流'', ''Logistics'', ''Hậu cần'', ''โลจิสติกส์'', ''Logistik'', ''Logistik'', ''ភស្តុភារ'', ''物流'', NULL, ''货物跨境运输、仓储与配送体系'', ''广西公共数据开放平台·东盟术语库''),
  (''集装箱'', ''Container'', ''Container'', ''ตู้คอนเทนเนอร์'', ''Kontena'', ''Kontainer'', ''ធុង'', ''物流'', NULL, ''标准化跨境货运装载单元'', ''广西公共数据开放平台·东盟术语库''),
  (''付款'', ''Payment'', ''Thanh toán'', ''การชำระเงิน'', ''Pembayaran'', ''Pembayaran'', ''ការទូទាត់'', ''支付'', NULL, ''跨境贸易货款结算行为'', ''广西公共数据开放平台·东盟术语库''),
  (''信用证'', ''Letter of Credit'', ''Thư tín dụng'', ''เงื่อนไขเครดิต'', ''Surat Kredit'', ''Surat Kredit'', ''លិខិតឥណទាន'', ''支付'', NULL, ''银行开立的跨境付款担保工具'', ''广西公共数据开放平台·东盟术语库''),
  (''合同'', ''Contract'', ''Hợp đồng'', ''สัญญา'', ''Kontrak'', ''Kontrak'', ''កិច្ចសន្យា'', ''法律'', NULL, ''明确贸易双方权利义务的协议'', ''广西公共数据开放平台·东盟术语库'')',
  'SELECT 1');
PREPARE stmt2 FROM @ins;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SELECT COUNT(*) AS total_terms,
       SUM(IF(term_my IS NOT NULL,1,0)) AS with_my,
       SUM(IF(term_in IS NOT NULL,1,0)) AS with_in,
       SUM(IF(term_km IS NOT NULL,1,0)) AS with_km
FROM t_trade_terminology;
