package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.DatasourceConfig;
import com.risk.mapper.DatasourceConfigMapper;
import com.risk.service.ExternalDataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

/**
 * 外部数据源对接控制器
 * 用于对接海关、税务、工商等外部API
 */
@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {

    private final RestTemplate restTemplate;

    @Autowired
    private ExternalDataSyncService syncService;

    @Autowired
    private DatasourceConfigMapper datasourceConfigMapper;

    public DataSourceController() {
        // 使用 Spring 自带的 SimpleClientHttpRequestFactory 配置超时，
        // 避免外部 API 无响应时 HTTP 线程无限挂起
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
    }

    // 从配置文件读取API地址和密钥
    @Value("${external-api.customs.url:}")
    private String customsApiUrl;

    @Value("${external-api.customs.key:}")
    private String customsApiKey;

    @Value("${external-api.tax.url:}")
    private String taxApiUrl;

    @Value("${external-api.tax.key:}")
    private String taxApiKey;

    @Value("${external-api.business.url:}")
    private String businessApiUrl;

    @Value("${external-api.business.key:}")
    private String businessApiKey;

    /**
     * 测试数据源连接
     * POST /api/datasource/test
     * body: { "type": "customs|tax|business", "url": "...", "apiKey": "..." }
     */
    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> testConnection(@RequestBody Map<String, String> params) {
        String url = params.get("url");
        String apiKey = params.get("apiKey");

        Map<String, Object> result = new HashMap<>();

        // SSRF 防护：校验 URL 协议与主机，禁止访问内网/回环/云元数据地址
        String rejectReason = validateExternalUrl(url);
        if (rejectReason != null) {
            result.put("success", false);
            result.put("message", rejectReason);
            return ApiResponse.success(result);
        }

        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
            }

            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            // 发送测试请求（只请求首页或ping接口）
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            result.put("success", true);
            result.put("statusCode", response.getStatusCodeValue());
            result.put("message", "连接成功");
            return ApiResponse.success(result);

        } catch (Exception e) {
            // 不回传原始异常信息，避免泄露内网拓扑/堆栈细节
            result.put("success", false);
            result.put("message", "连接失败，请检查目标地址是否可达");
            return ApiResponse.success(result);
        }
    }

    /**
     * SSRF 防护：校验外部 URL。
     * 返回 null 表示允许访问；返回非 null 字符串表示拒绝原因。
     * 规则：
     * 1. 协议仅允许 http / https；
     * 2. 主机禁止 localhost、回环(127.0.0.0/8)、内网(10/8、172.16/12、192.168/16)、
     *    链路本地(169.254/16，含云元数据 169.254.169.254)、本机标识。
     */
    private String validateExternalUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "URL 不能为空";
        }
        java.net.URI uri;
        try {
            uri = new java.net.URI(url.trim());
        } catch (Exception e) {
            return "URL 格式不合法";
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return "仅允许 http/https 协议";
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return "URL 缺少主机地址";
        }
        String h = host.toLowerCase();
        if (h.equals("localhost") || h.endsWith(".localhost") || h.equals("ip6-localhost")) {
            return "禁止访问本机地址";
        }
        try {
            for (java.net.InetAddress addr : java.net.InetAddress.getAllByName(host)) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                    return "禁止访问内网/回环/链路本地地址";
                }
                // 兜底：显式拦截云元数据地址 169.254.169.254
                if (addr.getHostAddress().startsWith("169.254.")) {
                    return "禁止访问云元数据地址";
                }
            }
        } catch (Exception e) {
            return "主机地址解析失败";
        }
        return null;
    }

    /**
     * 同步海关数据
     * POST /api/datasource/sync/customs
     */
    @PostMapping("/sync/customs")
    public ApiResponse<Map<String, Object>> syncCustomsData(@RequestBody(required = false) Map<String, Object> params) {
        return ApiResponse.success(syncService.syncCustoms());
    }

    /**
     * 同步税务数据
     * POST /api/datasource/sync/tax
     */
    @PostMapping("/sync/tax")
    public ApiResponse<Map<String, Object>> syncTaxData(@RequestBody(required = false) Map<String, Object> params) {
        return ApiResponse.success(syncService.syncTax());
    }

    /**
     * 同步工商数据
     * POST /api/datasource/sync/business
     */
    @PostMapping("/sync/business")
    public ApiResponse<Map<String, Object>> syncBusinessData(@RequestBody(required = false) Map<String, Object> params) {
        return ApiResponse.success(syncService.syncBusiness());
    }

    /**
     * 同步东盟11国宏观经济真实指标（World Bank Open Data，含东帝汶）
     * POST /api/datasource/sync/asean-macro
     */
    @PostMapping("/sync/asean-macro")
    public ApiResponse<Map<String, Object>> syncAseanMacro() {
        return ApiResponse.success(syncService.syncAseanMacro());
    }

    /**
     * 获取配置的数据源列表
     * GET /api/datasource/configs
     */
    @GetMapping("/configs")
    public ApiResponse<List<Map<String, Object>>> getDatasourceConfigs() {
        Map<String, Map<String, Object>> defaults = new LinkedHashMap<>();
        defaults.put("customs", buildConfig("customs", "海关数据接口", customsApiUrl, "未配置", ""));
        defaults.put("tax", buildConfig("tax", "税务数据接口", taxApiUrl, "未配置", ""));
        defaults.put("business", buildConfig("business", "工商数据接口", businessApiUrl, "未配置", ""));

        for (DatasourceConfig saved : datasourceConfigMapper.findAll()) {
            Map<String, Object> item = buildConfig(saved.getType(), saved.getName(), saved.getUrl(),
                    saved.getStatus(), saved.getRemark());
            item.put("configured", saved.getUrl() != null && !saved.getUrl().isEmpty());
            item.put("hasApiKey", saved.getApiKey() != null && !saved.getApiKey().isEmpty());
            defaults.put(saved.getType(), item);
        }

        return ApiResponse.success(new ArrayList<>(defaults.values()));
    }

    /**
     * 保存数据源配置
     * POST /api/datasource/configs
     */
    @PostMapping("/configs")
    public ApiResponse<String> saveDatasourceConfigs(@RequestBody Map<String, Object> config) {
        DatasourceConfig entity = new DatasourceConfig();
        entity.setType(String.valueOf(config.getOrDefault("type", "")).trim());
        entity.setName(String.valueOf(config.getOrDefault("name", defaultName(entity.getType()))).trim());
        entity.setUrl(String.valueOf(config.getOrDefault("url", "")).trim());
        entity.setApiKey(String.valueOf(config.getOrDefault("apiKey", "")).trim());
        entity.setStatus(String.valueOf(config.getOrDefault("status", "已配置")).trim());
        entity.setRemark(String.valueOf(config.getOrDefault("remark", "用户配置的数据源")).trim());

        if (!Arrays.asList("customs", "tax", "business").contains(entity.getType())) {
            return ApiResponse.error("数据源类型仅支持 customs/tax/business");
        }
        String rejectReason = validateConfigUrl(entity.getUrl());
        if (rejectReason != null) {
            return ApiResponse.error(rejectReason);
        }
        datasourceConfigMapper.upsert(entity);
        return ApiResponse.success("配置已保存并持久化");
    }

    private String validateConfigUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "URL 不能为空";
        }
        java.net.URI uri;
        try {
            uri = new java.net.URI(url.trim());
        } catch (Exception e) {
            return "URL 格式不合法";
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return "仅允许 http/https 协议";
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return "URL 缺少主机地址";
        }
        String h = host.toLowerCase();
        if (h.equals("localhost") || h.endsWith(".localhost") || h.equals("ip6-localhost")) {
            return "禁止保存本机地址";
        }
        return null;
    }

    private Map<String, Object> buildConfig(String type, String name, String url, String status, String remark) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("name", name != null && !name.isEmpty() ? name : defaultName(type));
        item.put("url", url != null ? url : "");
        item.put("status", status != null && !status.isEmpty() ? status : "未配置");
        item.put("remark", remark != null ? remark : "");
        item.put("configured", false);
        item.put("hasApiKey", false);
        return item;
    }

    private String defaultName(String type) {
        if ("customs".equals(type)) return "海关数据接口";
        if ("tax".equals(type)) return "税务数据接口";
        if ("business".equals(type)) return "工商数据接口";
        return "外部数据接口";
    }
}
