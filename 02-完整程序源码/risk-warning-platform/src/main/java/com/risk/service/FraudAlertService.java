package com.risk.service;

import com.risk.entity.FraudAlert;
import com.risk.mapper.FraudAlertMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudAlertService {

    @Autowired
    private FraudAlertMapper fraudAlertMapper;

    /** 按信用代码查询预警 */
    public List<FraudAlert> getAlertsByCreditCode(String creditCode) {
        return fraudAlertMapper.selectByCreditCode(creditCode);
    }

    /** 获取未确认预警数量 */
    public int getUnconfirmedCount() {
        return fraudAlertMapper.countUnconfirmed();
    }

    /** 确认预警 */
    public int confirmAlert(Long id, Integer status, String handler, String result) {
        return fraudAlertMapper.updateConfirmation(id, status, handler, result, LocalDateTime.now());
    }

    /** 分页查询 */
    public List<FraudAlert> findAll(int offset, int limit) {
        return fraudAlertMapper.findAll(offset, limit, "statusPriority", "asc");
    }

    /** 总数 */
    public int countAll() {
        return fraudAlertMapper.countAll();
    }

    /** 更新状态 */
    public int updateStatus(Long id, Integer status) {
        return fraudAlertMapper.updateStatus(id, status);
    }
}
