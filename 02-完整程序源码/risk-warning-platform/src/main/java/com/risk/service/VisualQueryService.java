package com.risk.service;

import com.risk.dto.QueryColumnMeta;
import com.risk.dto.QueryFilter;
import com.risk.dto.QuerySourceMeta;
import com.risk.dto.VisualQueryRequest;
import com.risk.mapper.VisualQueryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 可视化查询（轻量低代码）核心服务。
 *
 * 设计要点：
 * 1. 数据源、字段、操作符全部来自服务端「白名单」注册表，用户无法注入任意列名/表名；
 * 2. 仅过滤条件的值经过 #{} 参数化绑定，从根本上杜绝 SQL 注入；
 * 3. 表格模式：SELECT 选定列 WHERE 过滤 LIMIT 200；
 *    图表模式：按指定维度 GROUP BY 计数，输出 (category, value)，便于前端渲染柱状/折线/饼图。
 */
@Service
public class VisualQueryService {

    @Autowired
    private VisualQueryMapper mapper;

    /* ===================== 白名单注册表 ===================== */

    private static class ColumnDef {
        String field;
        String label;
        String type;          // string / number / date / enum
        boolean groupable;
        boolean defaultDisplay;
        List<String> options;  // 枚举可选值

        ColumnDef(String field, String label, String type, boolean groupable, boolean defaultDisplay, String... options) {
            this.field = field;
            this.label = label;
            this.type = type;
            this.groupable = groupable;
            this.defaultDisplay = defaultDisplay;
            this.options = options.length > 0 ? Arrays.asList(options) : null;
        }
    }

    private static class SourceDef {
        String key;
        String name;
        String description;
        String table;
        List<ColumnDef> columns = new ArrayList<>();
    }

    private static final Set<String> STRING_OPS = Set.of("eq", "ne", "like");
    private static final Set<String> NUMBER_OPS = Set.of("eq", "ne", "gt", "gte", "lt", "lte");
    private static final Set<String> DATE_OPS   = Set.of("eq", "gte", "lte", "like");
    private static final Set<String> ENUM_OPS   = Set.of("eq");

    private final Map<String, SourceDef> sources = buildRegistry();

    private Map<String, SourceDef> buildRegistry() {
        Map<String, SourceDef> map = new LinkedHashMap<>();

        // 1. 企业信息
        SourceDef enterprise = new SourceDef();
        enterprise.key = "enterprise";
        enterprise.name = "企业基础信息";
        enterprise.description = "市场监管公开数据：企业资质、行业、口岸关联等";
        enterprise.table = "t_enterprise";
        enterprise.columns.add(new ColumnDef("credit_code", "统一社会信用代码", "string", false, true));
        enterprise.columns.add(new ColumnDef("enterprise_name", "企业名称", "string", false, true));
        enterprise.columns.add(new ColumnDef("legal_person", "法定代表人", "string", false, false));
        enterprise.columns.add(new ColumnDef("registered_capital", "注册资本(万元)", "number", false, true));
        enterprise.columns.add(new ColumnDef("industry_category", "行业类别", "string", true, true));
        enterprise.columns.add(new ColumnDef("primary_port", "主要关联口岸", "string", true, true));
        enterprise.columns.add(new ColumnDef("trade_volume", "年交易额(万元)", "number", false, true));
        enterprise.columns.add(new ColumnDef("establish_years", "成立年限", "number", false, false));
        map.put(enterprise.key, enterprise);

        // 2. 商事纠纷案例
        SourceDef dispute = new SourceDef();
        dispute.key = "dispute";
        dispute.name = "商事纠纷案例";
        dispute.description = "跨境商事纠纷案例库：类型、品类、涉及国家等";
        dispute.table = "t_dispute_case";
        dispute.columns.add(new ColumnDef("case_no", "案例编号", "string", false, true));
        dispute.columns.add(new ColumnDef("title", "案例标题", "string", false, true));
        dispute.columns.add(new ColumnDef("case_type", "纠纷类型", "enum", true, true,
                "合同纠纷", "知识产权", "产品质量", "支付纠纷"));
        dispute.columns.add(new ColumnDef("trade_category", "贸易品类", "string", true, true));
        dispute.columns.add(new ColumnDef("countries", "涉及国家", "string", true, false));
        dispute.columns.add(new ColumnDef("court", "审理法院", "string", false, false));
        dispute.columns.add(new ColumnDef("publish_date", "发布日期", "date", false, true));
        map.put(dispute.key, dispute);

        // 3. 口岸通关记录
        SourceDef customs = new SourceDef();
        customs.key = "customs";
        customs.name = "口岸通关记录";
        customs.description = "广西口岸办公开统计：申报、查验、通关时长等";
        customs.table = "t_customs_declaration";
        customs.columns.add(new ColumnDef("credit_code", "统一社会信用代码", "string", false, false));
        customs.columns.add(new ColumnDef("enterprise_name", "企业名称", "string", false, true));
        customs.columns.add(new ColumnDef("port_name", "口岸名称", "string", true, true));
        customs.columns.add(new ColumnDef("declaration_type", "申报类型", "enum", true, true, "进口", "出口"));
        customs.columns.add(new ColumnDef("goods_category", "商品品类", "string", true, true));
        customs.columns.add(new ColumnDef("is_violation", "是否违规", "enum", true, true, "0", "1"));
        customs.columns.add(new ColumnDef("inspection_result", "查验结果", "string", true, true));
        customs.columns.add(new ColumnDef("clearance_duration", "通关时长(小时)", "number", false, true));
        customs.columns.add(new ColumnDef("declaration_date", "申报日期", "date", false, false));
        map.put(customs.key, customs);

        // 4. 跨境结算信用
        SourceDef settlement = new SourceDef();
        settlement.key = "settlement";
        settlement.name = "跨境结算信用";
        settlement.description = "人民银行南宁中心支行公示：结算、币种、异常等";
        settlement.table = "t_settlement_credit";
        settlement.columns.add(new ColumnDef("credit_code", "统一社会信用代码", "string", false, false));
        settlement.columns.add(new ColumnDef("enterprise_name", "企业名称", "string", false, true));
        settlement.columns.add(new ColumnDef("settlement_type", "结算类型", "string", true, true));
        settlement.columns.add(new ColumnDef("settlement_currency", "结算币种", "string", true, true));
        settlement.columns.add(new ColumnDef("settlement_amount", "结算金额(万元)", "number", false, true));
        settlement.columns.add(new ColumnDef("settlement_direction", "资金方向", "enum", true, true, "收入", "支出"));
        settlement.columns.add(new ColumnDef("target_country", "对手方所在国", "string", true, false));
        settlement.columns.add(new ColumnDef("is_abnormal", "是否异常", "enum", true, true, "0", "1"));
        settlement.columns.add(new ColumnDef("credit_score", "信用评分", "number", false, true));
        settlement.columns.add(new ColumnDef("settlement_date", "结算日期", "date", false, false));
        map.put(settlement.key, settlement);

        // 5. 分国别风控
        SourceDef countryRisk = new SourceDef();
        countryRisk.key = "countryRisk";
        countryRisk.name = "分国别风控配置";
        countryRisk.description = "东盟各国风控权重与重点监管领域配置";
        countryRisk.table = "t_country_risk_config";
        countryRisk.columns.add(new ColumnDef("country_code", "国家代码", "string", true, true));
        countryRisk.columns.add(new ColumnDef("country_name", "国家名称", "string", true, true));
        countryRisk.columns.add(new ColumnDef("basic_weight", "基础资质权重", "number", false, false));
        countryRisk.columns.add(new ColumnDef("compliance_weight", "经营合规权重", "number", false, false));
        countryRisk.columns.add(new ColumnDef("customs_weight", "口岸通关权重", "number", false, false));
        countryRisk.columns.add(new ColumnDef("trade_weight", "贸易活跃权重", "number", false, false));
        countryRisk.columns.add(new ColumnDef("adjust_weight", "综合调整权重", "number", false, false));
        countryRisk.columns.add(new ColumnDef("is_active", "是否启用", "enum", true, true, "0", "1"));
        map.put(countryRisk.key, countryRisk);

        // 6. 欺诈预警
        SourceDef fraud = new SourceDef();
        fraud.key = "fraud";
        fraud.name = "欺诈预警记录";
        fraud.description = "时序预测模型生成的跨境欺诈事前预警";
        fraud.table = "t_fraud_alert";
        fraud.columns.add(new ColumnDef("credit_code", "统一社会信用代码", "string", false, false));
        fraud.columns.add(new ColumnDef("enterprise_name", "企业名称", "string", false, true));
        fraud.columns.add(new ColumnDef("fraud_type", "欺诈类型", "string", true, true));
        fraud.columns.add(new ColumnDef("risk_level", "风险等级", "enum", true, true, "红色预警", "橙色预警", "黄色关注"));
        fraud.columns.add(new ColumnDef("risk_probability", "风险概率", "number", false, true));
        fraud.columns.add(new ColumnDef("is_confirmed", "核实状态", "enum", true, true, "0", "1", "2"));
        fraud.columns.add(new ColumnDef("prediction_period", "预测窗口", "string", true, false));
        fraud.columns.add(new ColumnDef("data_source", "数据来源", "string", false, false));
        map.put(fraud.key, fraud);

        return map;
    }

    /* ===================== 对外接口 ===================== */

    public List<QuerySourceMeta> listSources() {
        return sources.values().stream().map(this::toMeta).collect(Collectors.toList());
    }

    public Map<String, Object> execute(VisualQueryRequest req) {
        if (req == null || req.getSource() == null || req.getSource().isEmpty()) {
            throw new IllegalArgumentException("请选择数据源");
        }
        SourceDef def = sources.get(req.getSource());
        if (def == null) {
            throw new IllegalArgumentException("未知数据源: " + req.getSource());
        }

        String chartType = req.getChartType() == null ? "table" : req.getChartType();
        if (!Set.of("table", "bar", "line", "pie").contains(chartType)) {
            throw new IllegalArgumentException("不支持的展示方式: " + chartType);
        }

        // 1) 校验并构造过滤条件（白名单）
        List<QueryFilter> filters = new ArrayList<>();
        if (req.getFilters() != null) {
            for (Map<String, String> f : req.getFilters()) {
                String field = f.get("field");
                String op = f.get("op");
                String value = f.get("value");
                if (field == null || field.isEmpty() || op == null || op.isEmpty()
                        || value == null || value.trim().isEmpty()) {
                    continue; // 跳过不完整条件
                }
                ColumnDef col = findColumn(def, field);
                if (col == null) {
                    throw new IllegalArgumentException("未知字段: " + field);
                }
                if (!allowedOps(col.type).contains(op)) {
                    throw new IllegalArgumentException("字段【" + col.label + "】不支持操作符: " + op);
                }
                QueryFilter qf = new QueryFilter();
                qf.setColumn(col.field);
                qf.setSqlOp(mapOp(op));
                qf.setParamValue(op.equals("like") ? "%" + value.trim() + "%" : value.trim());
                filters.add(qf);
            }
        }

        // 2) 构造输出列 / 分组子句（白名单）
        String columnsSql;
        String groupByClause;
        List<QueryColumnMeta> outputColumns = new ArrayList<>();

        if ("table".equals(chartType)) {
            List<String> selected = req.getColumns();
            List<ColumnDef> chosen;
            if (selected == null || selected.isEmpty()) {
                chosen = def.columns.stream().filter(c -> c.defaultDisplay).collect(Collectors.toList());
            } else {
                chosen = new ArrayList<>();
                for (String f : selected) {
                    ColumnDef col = findColumn(def, f);
                    if (col == null) {
                        throw new IllegalArgumentException("未知输出列: " + f);
                    }
                    chosen.add(col);
                }
            }
            columnsSql = chosen.stream()
                    .map(c -> "`" + c.field + "`")
                    .collect(Collectors.joining(", "));
            groupByClause = "LIMIT 200";
            outputColumns = chosen.stream().map(this::toColMeta).collect(Collectors.toList());
        } else {
            String groupBy = req.getGroupBy();
            if (groupBy == null || groupBy.isEmpty()) {
                throw new IllegalArgumentException("图表模式必须选择分组字段");
            }
            ColumnDef gb = findColumn(def, groupBy);
            if (gb == null || !gb.groupable) {
                throw new IllegalArgumentException("字段【" + (gb == null ? groupBy : gb.label) + "】不可作为分组维度");
            }
            columnsSql = "`" + gb.field + "` AS category, COUNT(*) AS value";
            groupByClause = "GROUP BY `" + gb.field + "` ORDER BY value DESC LIMIT 50";
        }

        // 3) 执行查询
        List<Map<String, Object>> rows = mapper.query(columnsSql, def.table, filters, groupByClause);

        // 4) 组装响应
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mode", "table".equals(chartType) ? "table" : "chart");
        resp.put("chartType", chartType);
        resp.put("source", def.key);
        resp.put("total", rows.size());

        if ("table".equals(chartType)) {
            resp.put("columns", outputColumns);
            resp.put("rows", rows);
        } else {
            List<String> categories = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                categories.add(String.valueOf(r.get("category")));
                Object v = r.get("value");
                values.add(v instanceof Number ? (Number) v : 0);
            }
            Map<String, Object> chart = new LinkedHashMap<>();
            chart.put("categories", categories);
            chart.put("series", Collections.singletonList(values));
            resp.put("chart", chart);
            resp.put("rows", rows);
        }
        return resp;
    }

    /* ===================== 内部工具 ===================== */

    private Set<String> allowedOps(String type) {
        switch (type) {
            case "number": return NUMBER_OPS;
            case "date":   return DATE_OPS;
            case "enum":   return ENUM_OPS;
            default:       return STRING_OPS;
        }
    }

    private String mapOp(String op) {
        switch (op) {
            case "eq":   return "=";
            case "ne":   return "!=";
            case "like": return "LIKE";
            case "gt":   return ">";
            case "gte":  return ">=";
            case "lt":   return "<";
            case "lte":  return "<=";
            default: throw new IllegalArgumentException("不支持的操作符: " + op);
        }
    }

    private ColumnDef findColumn(SourceDef def, String field) {
        for (ColumnDef c : def.columns) {
            if (c.field.equals(field)) return c;
        }
        return null;
    }

    private QuerySourceMeta toMeta(SourceDef def) {
        QuerySourceMeta meta = new QuerySourceMeta();
        meta.setKey(def.key);
        meta.setName(def.name);
        meta.setDescription(def.description);
        meta.setColumns(def.columns.stream().map(this::toColMeta).collect(Collectors.toList()));
        return meta;
    }

    private QueryColumnMeta toColMeta(ColumnDef c) {
        QueryColumnMeta m = new QueryColumnMeta();
        m.setField(c.field);
        m.setLabel(c.label);
        m.setType(c.type);
        m.setGroupable(c.groupable);
        m.setDefaultDisplay(c.defaultDisplay);
        m.setOptions(c.options);
        return m;
    }
}
