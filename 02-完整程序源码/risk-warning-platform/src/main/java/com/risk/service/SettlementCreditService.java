package com.risk.service;

import com.risk.entity.SettlementCredit;
import com.risk.mapper.SettlementCreditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SettlementCreditService {

    @Autowired
    private SettlementCreditMapper settlementCreditMapper;

    /** 查询结算历史 */
    public List<SettlementCredit> getSettlementHistory(String creditCode) {
        return settlementCreditMapper.selectByCreditCode(creditCode);
    }

    /** 获取信用评分（平均分） */
    public Double getCreditScore(String creditCode) {
        return settlementCreditMapper.selectAverageCreditScoreByCreditCode(creditCode);
    }

    /** 获取所有异常记录 */
    public List<SettlementCredit> getAbnormalRecords() {
        return settlementCreditMapper.selectAbnormalRecords();
    }

    /** 分页查询 */
    public List<SettlementCredit> findAll(int offset, int limit) {
        return settlementCreditMapper.findAll(offset, limit);
    }

    /** 总数 */
    public int countAll() {
        return settlementCreditMapper.countAll();
    }

    /** 月度趋势 */
    public List<Map<String, Object>> monthlyTrend() {
        return settlementCreditMapper.monthlyTrend();
    }
}
