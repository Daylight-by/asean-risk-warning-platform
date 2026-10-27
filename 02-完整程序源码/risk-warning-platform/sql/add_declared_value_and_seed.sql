-- 用途：为跨境欺诈前置预警补齐“申报货值”特征。
-- 数据边界：以下值仅用于项目演示和模型可复现验证，不代表真实企业或海关申报金额。

-- 用 information_schema 判断列是否存在，兼容不支持 ADD COLUMN IF NOT EXISTS 的 MySQL 版本。
SET @declared_value_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 't_customs_declaration'
    AND column_name = 'declared_value'
);
SET @add_declared_value_sql = IF(
  @declared_value_column_exists = 0,
  'ALTER TABLE t_customs_declaration ADD COLUMN declared_value DECIMAL(16,2) NOT NULL DEFAULT 0 COMMENT ''申报货值（人民币元，项目演示样例）'' AFTER clearance_duration',
  'SELECT 1'
);
PREPARE add_declared_value_stmt FROM @add_declared_value_sql;
EXECUTE add_declared_value_stmt;
DEALLOCATE PREPARE add_declared_value_stmt;

-- 对历史演示报关记录按品类、进出口方向、月份生成稳定且可复现的样例货值。
-- 仅填补未填写的记录，后续接入真实开放数据时可直接覆盖该字段。
UPDATE t_customs_declaration
SET declared_value = ROUND(
  (CASE
     WHEN goods_category LIKE '%金属%' OR goods_category LIKE '%矿%' THEN 420000
     WHEN goods_category LIKE '%化工%' THEN 360000
     WHEN goods_category LIKE '%水果%' OR goods_category LIKE '%食品%' OR goods_category LIKE '%水产%' THEN 180000
     WHEN goods_category LIKE '%冷链%' THEN 260000
     ELSE 220000
   END)
  * (CASE WHEN declaration_type = '出口' THEN 1.18 ELSE 0.92 END)
  * (1 + MOD(id, 5) * 0.08)
  * (1 + MONTH(declaration_date) * 0.015), 2)
WHERE declared_value IS NULL OR declared_value = 0;
