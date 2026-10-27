-- ============================================
-- 补齐 t_trade_terminology 全部 25 条记录的马来/印尼/柬埔寨三语
-- 幂等：仅当三语为空时更新
-- ============================================
UPDATE t_trade_terminology SET term_my='Deklarasi Kastam', term_in='Deklarasi Pabean', term_km='សេចក្តីថ្លែងការណ៍គយ' WHERE id=1 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Sijil Asal', term_in='Sertifikat Asal', term_km='វិក្កយបត្របង្កើត' WHERE id=2 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Perdagangan Sempadan Skala Kecil', term_in='Perdagangan Lintas Batas Skala Kecil', term_km='ពាណិជ្ជកម្មតាមព្រំដែនខ្នាតតូច' WHERE id=3 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Pulangan Cukai Eksport', term_in='Pengembalian Pajak Ekspor', term_km='ការសងប្រាក់ពន្ធនាំចេញ' WHERE id=4 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Penyelesaian RMB Rentas Sempadan', term_in='Penyelesaian RMB Lintas Batas', term_km='ការទូទាត់ RMB ឆ្លងកាត់ព្រំដែន' WHERE id=5 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Kod HS', term_in='Kode HS', term_km='លេខកូដ HS' WHERE id=6 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Konosemen', term_in='Konosemen', term_km='លិខិតដឹកជញ្ជូន' WHERE id=7 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Surat Kredit', term_in='Surat Kredit', term_km='លិខិតឥណទាន' WHERE id=8 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Cukai Anti-Lambakan', term_in='Bea Masuk Anti-Dumping', term_km='ពន្ធប្រឆាំងការលក់តម្លៃទាប' WHERE id=9 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Perjanjian Perdagangan Bebas (FTA)', term_in='Perjanjian Perdagangan Bebas (FTA)', term_km='កិច្ចព្រមព្រៀងពាណិជ្ជកម្មសេរី' WHERE id=10 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Pemeriksaan dan Kuarantin', term_in='Pemeriksaan dan Karantina', term_km='ការត្រួតពិនិត្យ និងចត្តាសត្តរ' WHERE id=11 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Perdagangan Eksport Semula', term_in='Perdagangan Re-Ekspor', term_km='ពាណិជ្ជកម្មនាំចេញឡើងវិញ' WHERE id=12 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Pengesahan Pertukaran Asing', term_in='Verifikasi Valuta Asing', term_km='ការផ្ទៀងផ្ទាត់រូបិយប័ណ្ណបរទេស' WHERE id=13 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Klausa Arbitrase', term_in='Klausul Arbitrase', term_km='គោលការណ៍យល់ព្រមដោយអាជ្ញាកណ្តាល' WHERE id=14 AND (term_my IS NULL OR term_my='');
UPDATE t_trade_terminology SET term_my='Gudang Bon', term_in='Gudang Berikat', term_km='ឃ្លាំងសម្ភារគយ' WHERE id=15 AND (term_my IS NULL OR term_my='');

-- 验证
SELECT id, term_cn,
       IF(term_my IS NULL,'<空>','✓') AS my_ok,
       IF(term_in IS NULL,'<空>','✓') AS in_ok,
       IF(term_km IS NULL,'<空>','✓') AS km_ok
FROM t_trade_terminology ORDER BY id;
