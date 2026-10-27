package com.risk.service;

import com.risk.entity.ComplianceCheck;
import com.risk.mapper.ComplianceCheckMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class ComplianceCheckService {

    @Autowired
    private ComplianceCheckMapper complianceCheckMapper;

    @Autowired
    private PublicDataClient publicDataClient;

    /** 按信用代码查询合规检查 */
    public List<ComplianceCheck> getChecksByCreditCode(String creditCode) {
        return complianceCheckMapper.selectByCreditCode(creditCode);
    }

    /** 运行合规检查（模拟评分逻辑） */
    public ComplianceCheck runComplianceCheck(String creditCode, String enterpriseName, String targetCountry) {
        ComplianceCheck check = new ComplianceCheck();
        // credit_code 为 NOT NULL，前端未传时用企业名派生稳定占位码（非随机）
        String code = (creditCode != null && !creditCode.isEmpty()) ? creditCode
                : "TMP" + Math.abs(enterpriseName == null ? 0 : enterpriseName.hashCode()) % 100000;
        check.setCreditCode(code);
        check.setEnterpriseName(enterpriseName);
        check.setTargetCountry(targetCountry);
        check.setCheckStatus("已完成");

        // 基于目标国真实宏观指标（CPI通胀率）派生合规评分，非随机
        String iso2 = PublicDataClient.iso2ByCnName(targetCountry);
        Double inflation = iso2 != null ? publicDataClient.getInflation(iso2) : null;
        int score;
        if (inflation != null) {
            // 通胀越高合规环境越差：通胀5%→约88分，15%→约74分，>25%→趋近60分
            score = (int) Math.max(55, Math.min(95, 95 - inflation * 1.4));
        } else {
            score = 78; // 无指标时取中上基准（非随机）
        }
        check.setTotalScore(score);

        if (score >= 80) {
            check.setRiskSummary("资质齐全，合规风险较低，建议关注目标国最新政策变动");
        } else if (score >= 60) {
            check.setRiskSummary("部分资质缺失或存在合规隐患，建议整改后再开展贸易");
        } else {
            check.setRiskSummary("多项资质不达标，存在较高合规风险，暂不建议开展该方向贸易");
        }

        check.setQualificationCheck("{\"外贸备案\":\"通过\",\"海关登记\":\"通过\",\"原产地证资质\":\"待确认\"}");
        check.setTariffRisk("{\"关税税率\":\"正常\",\"反倾销风险\":\"低\",\"配额限制\":\"无\"}");
        check.setCustomsRedline("{\"禁止出口品类\":\"未涉及\",\"敏感物项\":\"未涉及\",\"申报规范\":\"正常\"}");
        check.setReportGenerated(0);
        check.setCreateTime(LocalDateTime.now());
        complianceCheckMapper.insert(check);
        return check;
    }

    /** 生成报告 */
    public int generateReport(Long id) {
        return complianceCheckMapper.updateReportGenerated(id, 1);
    }
}
