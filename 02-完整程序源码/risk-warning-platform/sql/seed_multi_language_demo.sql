USE risk_warning;

-- Demo data for competition presentation only.
-- Replace these aliases with verified public registry data before formal production use.
INSERT INTO t_multi_language_mapping
    (credit_code, name_cn, name_en, name_vn, name_th, name_my, name_id, name_kh, source_country)
VALUES
    ('91450681MA5PPA1D5N',
     '广西东兴淘一二三进出口贸易有限公司',
     'Guangxi Dongxing Tao123 Import and Export Trading Co., Ltd.',
     'Cong ty Xuat nhap khau Dongxing Tao123',
     'บริษัทนำเข้าและส่งออกตงซิงเถา123',
     'Syarikat Import Eksport Dongxing Tao123',
     'Perusahaan Impor Ekspor Dongxing Tao123',
     'ក្រុមហ៊ុននាំចេញនាំចូល Dongxing Tao123',
     'demo'),
    ('91450681315843133T',
     '东兴市中越泰国际食品有限公司',
     'Dongxing China-Vietnam-Thailand International Food Co., Ltd.',
     'Cong ty Thuc pham Quoc te Trung Viet Thai Dongxing',
     'บริษัทอาหารนานาชาติจีนเวียดนามไทยตงซิง',
     'Syarikat Makanan Antarabangsa China Vietnam Thailand Dongxing',
     'Perusahaan Makanan Internasional China Vietnam Thailand Dongxing',
     'ក្រុមហ៊ុនអាហារអន្តរជាតិចិនវៀតណាមថៃ Dongxing',
     'demo'),
    ('91450681MA5KL40R01',
     '柳州市振宇金属贸易有限公司东兴市分公司',
     'Liuzhou Zhenyu Metal Trading Dongxing Branch',
     'Chi nhanh Thuong mai Kim loai Zhenyu tai Dongxing',
     'สาขาตงซิง บริษัทค้าโลหะเจิ้นอวี่',
     'Cawangan Dongxing Perdagangan Logam Zhenyu',
     'Cabang Dongxing Perdagangan Logam Zhenyu',
     'សាខា Dongxing ពាណិជ្ជកម្មលោហៈ Zhenyu',
     'demo')
ON DUPLICATE KEY UPDATE
    name_cn = VALUES(name_cn),
    name_en = VALUES(name_en),
    name_vn = VALUES(name_vn),
    name_th = VALUES(name_th),
    name_my = VALUES(name_my),
    name_id = VALUES(name_id),
    name_kh = VALUES(name_kh),
    source_country = VALUES(source_country);
