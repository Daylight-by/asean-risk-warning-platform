package com.risk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公共开放数据客户端 —— 赛题「政府开放数据 / 公共数据融合利用」核心实现。
 *
 * <p>真实对接两类合法、免费、无版权的公开数据源：</p>
 * <ul>
 *   <li><b>World Bank Open Data</b>：各国宏观经济指标（GDP、商品贸易占GDP比、CPI通胀等），
 *       覆盖东盟11国（含东帝汶 Timór-Leste，2025 年正式加入东盟），API：{@code https://api.worldbank.org/v2}，{@code mrv=1} 取最新值。</li>
 *   <li><b>开放汇率 API</b>：{@code https://open.er-api.com/v6}，实时汇率，
 *       用于跨境人民币结算信用等模块。</li>
 * </ul>
 *
 * <p>东盟国家基础信息（本地名 / 货币 / 首都 / 官方语言）使用公开常识静态参考，
 * 避免依赖已废弃的 REST Countries v3 接口，保证稳定性。</p>
 *
 * <p>全部外部调用统一具备：内存缓存(30min)、超时(connect 3s / read 5s)、
 * 降级(失败返回 null，不向上抛)、slf4j 日志。</p>
 */
@Service
public class PublicDataClient {

    private static final Logger log = LoggerFactory.getLogger(PublicDataClient.class);
    private static final String WB_BASE = "https://api.worldbank.org/v2";
    private static final String FX_BASE = "https://open.er-api.com/v6";
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;

    /** World Bank 指标代码 */
    public static final String IND_GDP = "NY.GDP.MKTP.CD";           // GDP(现价美元)
    public static final String IND_TRADE_PCT = "TG.VAL.TOTL.GD.ZS";  // 商品贸易占GDP%
    public static final String IND_INFLATION = "FP.CPI.TOTL.ZG";     // CPI通胀%

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public PublicDataClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    // ===================== World Bank 指标 =====================

    /**
     * 查询某国某指标最新值（World Bank，mrv=1）。
     *
     * @param iso2           国家 ISO2 码（VN/TH/MY/ID/KH/LA/PH/SG/MM/BN）
     * @param indicatorCode  指标代码，见 {@link #IND_GDP} 等
     * @return 指标值；查询失败或无数据返回 null（不抛异常）
     */
    public Double getIndicator(String iso2, String indicatorCode) {
        if (iso2 == null || iso2.isEmpty() || indicatorCode == null) {
            return null;
        }
        String key = "wb:" + iso2 + ":" + indicatorCode;
        Double cached = get(key, Double.class);
        if (cached != null) {
            return cached;
        }
        try {
            String url = WB_BASE + "/country/" + iso2 + "/indicator/" + indicatorCode
                    + "?format=json&mrv=1";
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(body);
            // World Bank 返回 [meta, [records]]
            if (root != null && root.isArray() && root.size() >= 2
                    && root.get(1).isArray() && root.get(1).size() > 0) {
                JsonNode rec = root.get(1).get(0);
                JsonNode val = rec.get("value");
                if (val != null && !val.isNull()) {
                    Double d = val.asDouble();
                    put(key, d);
                    return d;
                }
            }
        } catch (Exception e) {
            log.warn("World Bank 指标查询失败 iso2={} ind={}: {}", iso2, indicatorCode, e.getMessage());
        }
        return null;
    }

    /** GDP（现价美元） */
    public Double getGdp(String iso2) {
        return getIndicator(iso2, IND_GDP);
    }

    /** 商品贸易占 GDP 百分比（衡量贸易开放度） */
    public Double getMerchandiseTradePct(String iso2) {
        return getIndicator(iso2, IND_TRADE_PCT);
    }

    /** CPI 通胀率 % */
    public Double getInflation(String iso2) {
        return getIndicator(iso2, IND_INFLATION);
    }

    // ===================== 汇率 =====================

    /**
     * 获取基准货币对各目标货币的实时汇率（开放汇率 API）。
     *
     * @param base 基准货币代码（USD / CNY 等）
     * @return 汇率映射（如 {"CNY":7.18, "VND":25400, ...}）；失败返回空 Map
     */
    public Map<String, Double> getExchangeRates(String base) {
        if (base == null || base.isEmpty()) {
            return Collections.emptyMap();
        }
        String key = "fx:" + base;
        Map<String, Double> cached = get(key, Map.class);
        if (cached != null) {
            return cached;
        }
        try {
            String body = restTemplate.getForObject(FX_BASE + "/latest/" + base, String.class);
            JsonNode root = mapper.readTree(body);
            JsonNode rates = root.path("rates");
            if (rates != null && rates.isObject()) {
                Map<String, Double> m = new HashMap<>();
                rates.fields().forEachRemaining(e -> {
                    try {
                        m.put(e.getKey(), e.getValue().asDouble());
                    } catch (Exception ignore) {
                        // 跳过无法解析的项
                    }
                });
                put(key, m);
                return m;
            }
        } catch (Exception e) {
            log.warn("汇率查询失败 base={}: {}", base, e.getMessage());
        }
        return Collections.emptyMap();
    }

    /** 获取 base→target 单一汇率；无数据返回 null。 */
    public Double getRate(String base, String target) {
        Map<String, Double> rates = getExchangeRates(base);
        if (rates == null || target == null) {
            return null;
        }
        return rates.get(target);
    }

    // ===================== 东盟国家基础信息（公开常识静态参考）=====================

    /** 获取东盟国家参考信息（本地名/货币/首都/官方语言）。 */
    public CountryRef getAseanCountryRef(String iso2) {
        if (iso2 == null) {
            return null;
        }
        return ASEAN_REFS.get(iso2);
    }

    /** 中文国名 → ISO2 码（便于业务用中文查找）。 */
    public static String iso2ByCnName(String cn) {
        if (cn == null) {
            return null;
        }
        switch (cn) {
            case "越南": return "VN";
            case "泰国": return "TH";
            case "马来西亚": return "MY";
            case "印尼":
            case "印度尼西亚": return "ID";
            case "柬埔寨": return "KH";
            case "老挝": return "LA";
            case "菲律宾": return "PH";
            case "新加坡": return "SG";
            case "缅甸": return "MM";
            case "文莱": return "BN";
            default: return null;
        }
    }

    /** 东盟国家参考信息（来源：各国外交/统计公开资料，公开常识，无版权）。 */
    public static class CountryRef {
        public final String iso2;
        public final String nameCn;
        public final String nameEn;
        public final String nameLocal;
        public final String currencyCode;
        public final String capital;
        public final String language;

        CountryRef(String iso2, String nameCn, String nameEn, String nameLocal,
                   String currencyCode, String capital, String language) {
            this.iso2 = iso2;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
            this.nameLocal = nameLocal;
            this.currencyCode = currencyCode;
            this.capital = capital;
            this.language = language;
        }
    }

    /** 东盟成员国基础参考表（含东帝汶，共 11 国） */
    private static final Map<String, CountryRef> ASEAN_REFS = Map.ofEntries(
            Map.entry("VN", new CountryRef("VN", "越南", "Viet Nam", "Việt Nam", "VND", "河内", "越南语")),
            Map.entry("TH", new CountryRef("TH", "泰国", "Thailand", "ประเทศไทย", "THB", "曼谷", "泰语")),
            Map.entry("MY", new CountryRef("MY", "马来西亚", "Malaysia", "Malaysia", "MYR", "吉隆坡", "马来语")),
            Map.entry("ID", new CountryRef("ID", "印尼", "Indonesia", "Indonesia", "IDR", "雅加达", "印尼语")),
            Map.entry("KH", new CountryRef("KH", "柬埔寨", "Cambodia", "កម្ពុជា", "KHR", "金边", "高棉语")),
            Map.entry("LA", new CountryRef("LA", "老挝", "Lao PDR", "ລາວ", "LAK", "万象", "老挝语")),
            Map.entry("PH", new CountryRef("PH", "菲律宾", "Philippines", "Pilipinas", "PHP", "马尼拉", "菲律宾语")),
            Map.entry("SG", new CountryRef("SG", "新加坡", "Singapore", "Singapore", "SGD", "新加坡", "英语/马来语/华语/泰米尔语")),
            Map.entry("MM", new CountryRef("MM", "缅甸", "Myanmar", "မြန်မာ", "MMK", "内比都", "缅甸语")),
            Map.entry("BN", new CountryRef("BN", "文莱", "Brunei", "Brunei", "BND", "斯里巴加湾市", "马来语")),
            Map.entry("TL", new CountryRef("TL", "东帝汶", "Timor-Leste", "Timór-Leste", "USD", "帝力", "葡萄牙语/德顿语"))
    );

    // ===================== 简易内存缓存（TTL 30min）=====================

    private static class CacheEntry {
        final Object value;
        final long expireAt;

        CacheEntry(Object v) {
            this.value = v;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String key, Class<T> type) {
        CacheEntry e = cache.get(key);
        if (e == null || System.currentTimeMillis() > e.expireAt) {
            cache.remove(key);
            return null;
        }
        return (T) e.value;
    }

    private void put(String key, Object value) {
        cache.put(key, new CacheEntry(value));
    }
}
