package com.risk.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.risk.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 风控助手控制器
 * -------------------------------------------------------------
 *   - provider = mock（默认）：基于业务上下文生成回复，逐字流式返回。
 *   - provider = ollama：转发到本地 Ollama，SSE delta 格式回传。
 *
 * 流式接口 POST /api/ai/chat/stream 使用 StreamingResponseBody：
 *   · 直接写入 OutputStream + flush，Spring 自动走 Transfer-Encoding: chunked
 *   · 与微信小程序 wx.request(enableChunked:true) + onChunkReceived 完美兼容
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Value("${ai.provider:mock}")
    private String provider;

    @Value("${ai.ollama.url:http://localhost:11434/api/chat}")
    private String ollamaUrl;

    @Value("${ai.ollama.model:qwen2:1.5b}")
    private String ollamaModel;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /* ========================= 非流式接口 ========================= */

    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String reply;
        if ("ollama".equals(provider)) {
            reply = chatFromOllama(body);
        } else {
            reply = buildMockReply(body);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", reply);
        return ApiResponse.success(data);
    }

    /** ollama 模式的非流式对话 */
    private String chatFromOllama(Map<String, Object> body) {
        try {
            Map<String, Object> context = asMap(body.get("context"));
            List<Map<String, Object>> history = asList(body.get("messages"));
            List<Map<String, Object>> ollamaMessages = new ArrayList<>();
            ollamaMessages.add(Map.of("role", "system", "content", buildSystemPrompt(context)));
            for (Map<String, Object> m : history) {
                ollamaMessages.add(Map.of(
                        "role", String.valueOf(m.getOrDefault("role", "user")),
                        "content", String.valueOf(m.getOrDefault("content", ""))
                ));
            }
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("model", ollamaModel);
            req.put("messages", ollamaMessages);
            req.put("stream", false);

            String ollamaEndpoint = sanitizeUrl(ollamaUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(req)))
                    .build();
            HttpResponse<java.io.InputStream> res = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                return "[Ollama 错误] 返回状态码: " + res.statusCode();
            }
            JSONObject obj = JSON.parseObject(res.body());
            JSONObject msg = obj.getJSONObject("message");
            return msg != null ? msg.getString("content") : "[Ollama 返回空内容]";
        } catch (Exception e) {
            return "[Ollama 不可用] " + e.getMessage() + "（已自动降级为 mock 回复）";
        }
    }

    /* ========================= 流式接口（StreamingResponseBody） ========================= */

    /**
     * 流式接口：POST /api/ai/chat/stream
     *
     * 使用 StreamingResponseBody —— Spring 专为流式输出设计的抽象：
     *   - 回调在独立线程执行，直接操作 HttpServletResponse 的 OutputStream
     *   - 自动使用 Transfer-Encoding: chunked（不计算 Content-Length）
     *   - 每次 write+flush 立即发送到网络，与 wx enableChunked 完美匹配
     *
     * 数据格式（每行一个 SSE event）：
     *   data: {"choices":[{"delta":{"content":"x"}]}
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> stream(@RequestBody Map<String, Object> body) {

        // 用 final 局部变量捕获实例字段，避免 lambda 闭包中引用可变字段
        final String prov = this.provider;
        final String oUrl = this.ollamaUrl;
        final String oModel = this.ollamaModel;
        final HttpClient http = this.httpClient;

        StreamingResponseBody streamBody = (OutputStream out) -> {
            try {
                if ("ollama".equals(prov)) {
                    doStreamOllama(body, oUrl, oModel, http, out);
                } else {
                    doStreamMock(body, out);
                }
            } catch (Exception e) {
                try {
                    writeChunk(out, "[Error] " + e.getMessage());
                } catch (Exception ignored) { /* ignore */ }
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "event-stream", StandardCharsets.UTF_8));
        headers.setCacheControl("no-cache");
        headers.set("X-Accel-Buffering", "no");

        return new ResponseEntity<>(streamBody, headers, org.springframework.http.HttpStatus.OK);
    }

    /* ========================= Ollama 流式实现 ========================= */

    private void doStreamOllama(Map<String, Object> body, String url,
                                String model, HttpClient http,
                                OutputStream out) throws Exception {
        Map<String, Object> context = asMap(body.get("context"));
        List<Map<String, Object>> history = asList(body.get("messages"));

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(context)));
        for (Map<String, Object> m : history) {
            messages.add(Map.of(
                    "role", String.valueOf(m.getOrDefault("role", "user")),
                    "content", String.valueOf(m.getOrDefault("content", ""))
            ));
        }

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", model);
        req.put("messages", messages);
        req.put("stream", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sanitizeUrl(url)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(req)))
                .build();

        HttpResponse<java.io.InputStream> res = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) {
            throw new RuntimeException("Ollama 返回状态码: " + res.statusCode());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                JSONObject obj = JSON.parseObject(line);
                JSONObject msg = obj.getJSONObject("message");
                String content = msg != null ? msg.getString("content") : "";
                if (content != null && !content.isEmpty()) {
                    writeChunk(out, content);
                }
            }
        }
    }

    /* ========================= Mock 流式实现 ========================= */

    private void doStreamMock(Map<String, Object> body, OutputStream out) throws Exception {
        String reply = buildMockReply(body);
        int i = 0;
        while (i < reply.length()) {
            int end = Math.min(i + 2, reply.length());
            writeChunk(out, reply.substring(i, end));
            i = end;
            Thread.sleep(18);
        }
    }

    /* ========================= 系统提示词 / Mock 回复 ========================= */

    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        String language = normalizeLanguage(context == null ? null : context.get("language"));
        if ("en".equals(language)) {
            sb.append("You are the AI risk assistant of the ASEAN Cross-border SME Risk Alert Platform for China–ASEAN trade users.\n");
            sb.append("Reply in concise, professional English. Focus on company risks, alerts, ASEAN trade policy and compliance.\n");
            sb.append("Use only the provided context. Keep company names, policy titles and case text in their original language; do not invent translations or facts.\n");
        } else if ("vi".equals(language)) {
            sb.append("Bạn là trợ lý AI của Nền tảng cảnh báo rủi ro doanh nghiệp vừa và nhỏ xuyên biên giới ASEAN, phục vụ thương mại Trung Quốc–ASEAN.\n");
            sb.append("Trả lời ngắn gọn, chuyên nghiệp bằng tiếng Việt, tập trung vào rủi ro doanh nghiệp, cảnh báo, chính sách thương mại ASEAN và tuân thủ.\n");
            sb.append("Chỉ sử dụng ngữ cảnh được cung cấp. Giữ nguyên ngôn ngữ gốc của tên doanh nghiệp, tiêu đề chính sách và nội dung vụ việc; không tự tạo dữ liệu hoặc bản dịch.\n");
        } else {
            sb.append("你是「东盟跨境中小商户智能风控预警平台」的 AI 风控助手，服务中国—东盟跨境贸易的中小商户与企业用户。\n");
            sb.append("要求：用简洁、专业、面向业务的中文回答，聚焦跨境企业风险、风险预警、东盟贸易政策与合规。\n");
            sb.append("重要：优先基于下方从平台数据库实时检索到的上下文作答，不要编造未提供的数据；数据不足时如实说明并建议用户使用对应功能模块。\n");
        }
        if (context != null) {
            sb.append("【检索上下文】\n");
            sb.append("意图类型: ").append(context.getOrDefault("intent", "general")).append("\n");
            sb.append("用户问题: ").append(context.getOrDefault("query", "")).append("\n");
            appendContextList(sb, "相关企业", asList(context.get("enterprises")));
            appendContextList(sb, "风险预警", asList(context.get("warnings")));
            appendContextList(sb, "相关政策", asList(context.get("policies")));
        }
        return sb.toString();
    }

    private String buildMockReply(Map<String, Object> body) {
        Map<String, Object> context = asMap(body.get("context"));
        if (context == null) context = new LinkedHashMap<>();
        String language = normalizeLanguage(context.get("language"));
        if (!"zh".equals(language)) return buildInternationalMockReply(context, language);
        String intent = String.valueOf(context.getOrDefault("intent", "general"));
        String query = String.valueOf(context.getOrDefault("query", ""));

        switch (intent) {
            case "enterprise": {
                List<Map<String, Object>> list = asList(context.get("enterprises"));
                if (list.isEmpty())
                    return "暂时没有匹配的企业数据，您可以换个关键词，或在「风控评分」中输入具体企业名称查询它的风险评分与信用报告。";
                String names = list.stream().limit(3)
                        .map(e -> String.valueOf(e.getOrDefault("enterpriseName", e.getOrDefault("name", "某跨境企业"))))
                        .collect(Collectors.joining("、"));
                return "根据平台融合的口岸与工商数据，为您找到以下相关跨境企业：" + names
                        + "。\n您可在「风控评分」中查看它们的详细风险评分、跨境结算信用分与信用报告；告诉我具体企业名称，我帮您进一步分析。";
            }
            case "warning": {
                List<Map<String, Object>> list = asList(context.get("warnings"));
                if (list.isEmpty())
                    return "当前暂无新的风险预警。建议您在「风险预警」中订阅重点企业，平台会在风险等级变化时第一时间提醒您。";
                String items = list.stream().limit(3)
                        .map(w -> "• " + w.getOrDefault("enterpriseName", "某企业") + "（" + w.getOrDefault("warningLevel", "风险") + "）")
                        .collect(Collectors.joining("\n"));
                return "近期平台监测到的风险动态：\n" + items
                        + "\n您可在「风险预警」查看详情并订阅提醒；需要某条预警的应对建议，告诉我即可。";
            }
            case "policy": {
                List<Map<String, Object>> list = asList(context.get("policies"));
                if (list.isEmpty())
                    return "暂无匹配的政策更新。您可以在「东盟政策」中按国家浏览全部政策与法规。";
                String items = list.stream().limit(3)
                        .map(p -> "• [" + p.getOrDefault("country", "东盟") + "] " + p.getOrDefault("title", p.getOrDefault("policyTitle", "")))
                        .collect(Collectors.joining("\n"));
                return "为您整理的近期相关政策：\n" + items
                        + "\n需要某条政策的详细解读或合规要点，告诉我即可。";
            }
            default:
                if (!query.isEmpty())
                    return "您提到的「" + query + "」我已记录。我是东盟风控 AI 助手，可以帮您：\n"
                            + "• 查询跨境企业风险与信用评分\n• 查看近期风险预警并订阅提醒\n"
                            + "• 检索东盟各国贸易政策与合规要点\n• 解读跨境贸易术语、纠纷案例与口岸画像\n试试问我「近期有哪些风险预警」吧～";
                return "我是东盟跨境中小商户智能风控预警平台的 AI 助手，可以帮您查询跨境企业风险、"
                        + "风险预警、东盟贸易政策与合规等内容。\n试试问我「帮我推荐几家优质跨境企业」或「最新东盟贸易政策有哪些？」";
        }
    }

    /** Mock provider keeps the UI language consistent while preserving source names/titles verbatim. */
    private String buildInternationalMockReply(Map<String, Object> context, String language) {
        boolean vi = "vi".equals(language);
        String intent = String.valueOf(context.getOrDefault("intent", "general"));
        String query = String.valueOf(context.getOrDefault("query", ""));
        switch (intent) {
            case "enterprise": {
                List<Map<String, Object>> list = asList(context.get("enterprises"));
                if (list.isEmpty()) {
                    return vi
                            ? "Chưa có doanh nghiệp phù hợp. Hãy thử từ khóa khác hoặc mở mục Điểm rủi ro để tra cứu theo tên doanh nghiệp chính xác."
                            : "No matching company was found. Try another keyword or use the Risk score page with an exact company name.";
                }
                String names = list.stream().limit(3)
                        .map(e -> String.valueOf(e.getOrDefault("enterpriseName", e.getOrDefault("name", vi ? "doanh nghiệp" : "company"))))
                        .collect(Collectors.joining(vi ? ", " : ", "));
                return vi
                        ? "Đã tìm thấy các doanh nghiệp liên quan từ dữ liệu cửa khẩu và đăng ký kinh doanh: " + names
                                + ". Xem thẻ bên dưới để mở điểm rủi ro và hồ sơ tín dụng."
                        : "Related companies found from port and business-registration data: " + names
                                + ". Use the card below to open risk scores and credit profiles.";
            }
            case "warning": {
                List<Map<String, Object>> list = asList(context.get("warnings"));
                if (list.isEmpty()) {
                    return vi
                            ? "Hiện chưa có cảnh báo mới. Bạn có thể đăng ký doanh nghiệp trọng điểm để nhận thông báo khi rủi ro thay đổi."
                            : "There are no new alerts at the moment. Subscribe to priority companies to be notified when risk changes.";
                }
                String items = list.stream().limit(3)
                        .map(w -> "• " + w.getOrDefault("enterpriseName", vi ? "Doanh nghiệp" : "Company")
                                + " (" + localizedRiskLevel(w.get("warningLevel"), language) + ")")
                        .collect(Collectors.joining("\n"));
                return (vi ? "Cảnh báo gần đây:\n" : "Recent risk alerts:\n") + items
                        + (vi ? "\nMở trang Cảnh báo rủi ro để xem chi tiết và đăng ký theo dõi."
                              : "\nOpen the Risk alerts page for details and subscriptions.");
            }
            case "policy": {
                List<Map<String, Object>> list = asList(context.get("policies"));
                if (list.isEmpty()) {
                    return vi
                            ? "Chưa có chính sách phù hợp. Bạn có thể duyệt Kho chính sách ASEAN theo quốc gia."
                            : "No matching policy updates were found. Browse the ASEAN policy library by country.";
                }
                String items = list.stream().limit(3)
                        .map(p -> "• [" + p.getOrDefault("country", "ASEAN") + "] "
                                + p.getOrDefault("title", p.getOrDefault("policyTitle", "")))
                        .collect(Collectors.joining("\n"));
                return (vi ? "Chính sách liên quan gần đây:\n" : "Recent related policies:\n") + items
                        + (vi ? "\nTiêu đề được giữ nguyên theo nguồn công bố; hãy mở chi tiết để xem dữ liệu nguồn."
                              : "\nTitles remain in the original publication language; open details to view the source record.");
            }
            default:
                if (query != null && !query.isEmpty()) {
                    return vi
                            ? "Đã ghi nhận yêu cầu “" + query + "”. Tôi có thể hỗ trợ tra cứu rủi ro doanh nghiệp, cảnh báo, chính sách ASEAN và tuân thủ."
                            : "Your request “" + query + "” was received. I can help with company risks, alerts, ASEAN policy and compliance.";
                }
                return vi
                        ? "Tôi có thể hỗ trợ tra cứu rủi ro doanh nghiệp, cảnh báo, chính sách ASEAN và tuân thủ xuyên biên giới."
                        : "I can help with company risks, alerts, ASEAN policy and cross-border compliance.";
        }
    }

    private String localizedRiskLevel(Object level, String language) {
        String value = String.valueOf(level == null ? "" : level).toLowerCase(Locale.ROOT);
        boolean high = value.contains("high") || value.contains("高");
        boolean medium = value.contains("medium") || value.contains("中");
        if ("vi".equals(language)) return high ? "Rủi ro cao" : medium ? "Rủi ro trung bình" : "Rủi ro thấp";
        return high ? "High risk" : medium ? "Medium risk" : "Low risk";
    }

    private String normalizeLanguage(Object value) {
        String language = String.valueOf(value == null ? "zh" : value).trim().toLowerCase(Locale.ROOT);
        return "en".equals(language) || "vi".equals(language) ? language : "zh";
    }

    /* ========================= 工具方法 ========================= */

    /** 写入一个 SSE 格式数据块并立即 flush */
    static void writeChunk(OutputStream out, String content) throws Exception {
        Map<String, Object> chunk = new LinkedHashMap<>();
        Map<String, Object> choice = new LinkedHashMap<>();
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("content", content);
        choice.put("delta", delta);
        chunk.put("choices", List.of(choice));
        String line = "data: " + JSON.toJSONString(chunk) + "\n\n";
        out.write(line.getBytes(StandardCharsets.UTF_8));
        out.flush();  // 关键：立即推送到网络
    }

    static String sanitizeUrl(String url) {
        if (url == null) return url;
        String u = url.trim();
        while (u.startsWith("-")) { u = u.substring(1).trim(); }
        return u;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> asList(Object o) {
        if (o instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object> >) o;
            return list;
        }
        return new ArrayList<>();
    }

    private void appendContextList(StringBuilder sb, String label, List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) return;
        sb.append(label).append(":\n");
        for (Map<String, Object> e : list) {
            sb.append("  - ").append(e.entrySet().stream()
                    .map(en -> en.getKey() + "=" + en.getValue())
                    .collect(Collectors.joining(", "))).append("\n");
        }
    }
}
