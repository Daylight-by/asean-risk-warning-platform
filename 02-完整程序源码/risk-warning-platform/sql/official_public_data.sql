-- Official public-data snapshots for the competition demo.
-- All values below are aggregate statistics. They are not enterprise-level records.
-- Source files and SHA-256 evidence are stored under E:\bisai\data when collected.

CREATE TABLE IF NOT EXISTS t_public_data_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(160) NOT NULL,
    source_url VARCHAR(700) NOT NULL,
    dataset_code VARCHAR(100) NOT NULL,
    metric_key VARCHAR(120) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    country_name VARCHAR(100) NOT NULL,
    reference_period VARCHAR(32) NOT NULL,
    metric_value DECIMAL(24,6) NOT NULL,
    unit VARCHAR(80) NOT NULL,
    data_status VARCHAR(32) NOT NULL DEFAULT 'official_public',
    retrieved_at DATETIME NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    provenance_note VARCHAR(500) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_official_snapshot (dataset_code, metric_key, country_code, reference_period),
    KEY idx_snapshot_country (country_code),
    KEY idx_snapshot_source (source_name),
    KEY idx_snapshot_period (reference_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Official public-data snapshots; aggregate statistics only';

-- UN Comtrade preview API, China (reporter 156) exports to ASEAN partners, HS TOTAL, 2023.
-- API URL: https://comtradeapi.un.org/public/v1/preview/C/A/HS
INSERT INTO t_public_data_snapshot
  (source_name, source_url, dataset_code, metric_key, country_code, country_name, reference_period, metric_value, unit, data_status, retrieved_at, content_sha256, provenance_note)
VALUES
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'BN', 'Brunei Darussalam', '2023', 857136430, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'KH', 'Cambodia', '2023', 12753001460, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'ID', 'Indonesia', '2023', 65195769414, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'LA', 'Lao PDR', '2023', 3350822924, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'MY', 'Malaysia', '2023', 87369777322, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'MM', 'Myanmar', '2023', 11401007018, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'PH', 'Philippines', '2023', 52400745910, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'SG', 'Singapore', '2023', 76945384255, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'TH', 'Thailand', '2023', 75732738143, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'VN', 'Viet Nam', '2023', 137607205249, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.'),
  ('UN Comtrade', 'https://comtradeapi.un.org/public/v1/preview/C/A/HS?period=2023&reporterCode=156&flowCode=X&partnerCode=096,116,360,418,458,104,608,702,764,704,626&cmdCode=TOTAL&maxRecords=100', 'uncomtrade_china_asean_exports', 'primary_value', 'TL', 'Timor-Leste', '2023', 261792339, 'USD', 'official_public', '2026-08-08 00:22:37', '9FF2F2CC47C16663635E2DFAF515A650A6446D7D50B06D7F79A1397AC58F1C50', 'UN Comtrade aggregate preview; reporter=China, flow=exports, commodity=TOTAL.')
ON DUPLICATE KEY UPDATE metric_value=VALUES(metric_value), retrieved_at=VALUES(retrieved_at), content_sha256=VALUES(content_sha256), provenance_note=VALUES(provenance_note), update_time=CURRENT_TIMESTAMP;

-- ASEANStats Data Portal, international sea cargo throughput, 2023, unit: thousand tons.
INSERT INTO t_public_data_snapshot
  (source_name, source_url, dataset_code, metric_key, country_code, country_name, reference_period, metric_value, unit, data_status, retrieved_at, content_sha256, provenance_note)
VALUES
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'BN', 'Brunei Darussalam', '2023', 1091, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.'),
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'ID', 'Indonesia', '2023', 85017, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.'),
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'KH', 'Cambodia', '2023', 7893, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.'),
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'MM', 'Myanmar', '2023', 36228, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.'),
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'MY', 'Malaysia', '2023', 586564.79284413, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.'),
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'PH', 'Philippines', '2023', 186596.878, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.'),
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'SG', 'Singapore', '2023', 592009, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.'),
  ('ASEANStats Data Portal', 'https://data.aseanstats.org/api/indicator/ASE.TRP.WTR.C.215', 'aseanstats_sea_cargo', 'international_sea_cargo_throughput', 'TH', 'Thailand', '2023', 368363, 'thousand_ton', 'official_public', '2026-08-08 00:23:00', 'C787886AC0CCDA5413979F945337BC14ECDB54DA25566E6DE8B77D3B3692883D', 'Official aggregate indicator; source response contains no 2023 record for every ASEAN country.')
ON DUPLICATE KEY UPDATE metric_value=VALUES(metric_value), retrieved_at=VALUES(retrieved_at), content_sha256=VALUES(content_sha256), provenance_note=VALUES(provenance_note), update_time=CURRENT_TIMESTAMP;
