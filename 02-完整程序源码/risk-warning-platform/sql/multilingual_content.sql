-- 多语种可追溯内容表：不覆盖原始业务记录
CREATE TABLE IF NOT EXISTS t_multilingual_content (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(32) NOT NULL COMMENT 'policy/warning/enterprise/case',
  business_id VARCHAR(64) NOT NULL,
  field_name VARCHAR(64) NOT NULL,
  source_language VARCHAR(16) NOT NULL DEFAULT 'zh',
  source_text TEXT NOT NULL,
  locale VARCHAR(16) NOT NULL,
  translated_text TEXT NOT NULL,
  translation_status VARCHAR(32) NOT NULL DEFAULT 'demo_ai_assisted',
  verification_status VARCHAR(32) NOT NULL DEFAULT 'pending_verification',
  translated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_multilingual_content (business_type, business_id, field_name, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多语种译文与原文追溯表';
