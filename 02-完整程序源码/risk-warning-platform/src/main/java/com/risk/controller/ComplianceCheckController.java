package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.ComplianceCheck;
import com.risk.service.ComplianceCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/compliance")
@CrossOrigin
public class ComplianceCheckController {

    @Autowired
    private ComplianceCheckService complianceCheckService;

    /** 查询企业合规检查 GET /api/compliance/{creditCode} */
    @GetMapping("/{creditCode}")
    public ApiResponse<List<ComplianceCheck>> getChecks(@PathVariable String creditCode) {
        List<ComplianceCheck> checks = complianceCheckService.getChecksByCreditCode(creditCode);
        return ApiResponse.success(checks);
    }

    /** 运行合规检查 POST /api/compliance/check */
    @PostMapping("/check")
    public ApiResponse<Map<String, Object>> runCheck(@RequestBody Map<String, String> body) {
        String creditCode = body.get("creditCode");
        String enterpriseName = body.get("name");
        String targetCountry = body.get("country");
        if (targetCountry == null) targetCountry = "";

        ComplianceCheck check = complianceCheckService.runComplianceCheck(creditCode, enterpriseName, targetCountry);
        if (check == null) {
            return ApiResponse.error("合规检查执行失败");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", check.getTotalScore());
        
        // 资质审查结果
        List<Map<String, Object>> qualification = new ArrayList<>();
        qualification.add(Map.of("item", "营业执照有效性", "pass", true, "remark", "样例字段初筛通过，待市场监管权威接口复核"));
        qualification.add(Map.of("item", "进出口经营资质", "pass", true, "remark", "样例字段初筛通过，待商务部门公开数据复核"));
        qualification.add(Map.of("item", "海关注册登记", "pass", true, "remark", "样例字段初筛通过，待海关权威接口复核"));
        boolean forexPass = check.getTotalScore() != null && check.getTotalScore() >= 70;
        qualification.add(Map.of("item", "外汇收支名录", "pass", forexPass, "remark", forexPass ? "规则初筛通过，仍需权威名录核验" : "规则初筛未通过，需优先核验名录状态"));
        qualification.add(Map.of("item", targetCountry + "进口许可", "pass", false, "remark", "当前未接入目标国许可核验接口，状态待核验"));
        result.put("qualification", qualification);
        
        // 关税风险
        List<Map<String, Object>> tariffRisk = new ArrayList<>();
        tariffRisk.add(Map.of("category", "农产品", "hsCode", "1006.30", "rate", "5%", "risk", "低风险", "note", "适用RCEP优惠税率"));
        tariffRisk.add(Map.of("category", "纺织品", "hsCode", "6109.10", "rate", "15%", "risk", "中风险", "note", "需确认原产地规则"));
        tariffRisk.add(Map.of("category", "电子产品", "hsCode", "8517.12", "rate", "0%", "risk", "低风险", "note", "ITA协议免税"));
        tariffRisk.add(Map.of("category", "化工产品", "hsCode", "3204.17", "rate", "10%", "risk", "高风险", "note", "涉及反倾销调查清单"));
        result.put("tariffRisk", tariffRisk);
        
        // 海关红线预警
        List<Map<String, Object>> customsAlerts = new ArrayList<>();
        customsAlerts.add(Map.of("type", "价格预警", "level", "红色", "desc", "申报价格低于海关参考价30%以上，可能被估价质疑"));
        customsAlerts.add(Map.of("type", "归类风险", "level", "橙色", "desc", "商品归类存在争议，建议申请预归类裁定"));
        customsAlerts.add(Map.of("type", "原产地规则", "level", "黄色", "desc", "需确保满足区域价值成分(RVC)40%以上要求"));
        customsAlerts.add(Map.of("type", "禁限管制", "level", "橙色", "desc", "目标国对该商品有进口配额限制"));
        result.put("customsAlerts", customsAlerts);
        
        return ApiResponse.success(result);
    }

    /** 生成报告 POST /api/compliance/{id}/report */
    @PostMapping("/{id}/report")
    public ApiResponse<String> generateReport(@PathVariable Long id) {
        int rows = complianceCheckService.generateReport(id);
        if (rows > 0) {
            return ApiResponse.success("报告生成成功");
        } else {
            return ApiResponse.error("报告生成失败");
        }
    }
}
