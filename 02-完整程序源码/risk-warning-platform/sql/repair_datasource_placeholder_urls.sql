-- Clear only known placeholder URLs left by the original demo configuration.
-- Do not overwrite operator-supplied URLs that are not in this allowlist.
SET NAMES utf8mb4;
UPDATE t_datasource_config
SET url = '', api_key = '', status = '未配置',
    remark = '未配置授权接口；真实公开统计通过 official-data 快照接口提供'
WHERE (type = 'customs' AND url IN ('https://api.customs.gov.cn/v2/declaration', 'https://data.gxzf.gov.cn/api/customs-demo'))
   OR (type = 'tax' AND url = 'https://api.chinatax.gov.cn/v3/enterprise')
   OR (type = 'business' AND url = 'https://api.gsxt.gov.cn/v1/company');
