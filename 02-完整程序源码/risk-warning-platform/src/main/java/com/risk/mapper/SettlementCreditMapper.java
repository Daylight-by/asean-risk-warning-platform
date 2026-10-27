package com.risk.mapper;

import com.risk.entity.SettlementCredit;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SettlementCreditMapper {

    @Select("SELECT * FROM t_settlement_credit WHERE credit_code = #{creditCode}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "creditCode", column = "credit_code"),
            @Result(property = "enterpriseName", column = "enterprise_name"),
            @Result(property = "settlementType", column = "settlement_type"),
            @Result(property = "settlementCurrency", column = "settlement_currency"),
            @Result(property = "settlementAmount", column = "settlement_amount"),
            @Result(property = "settlementDirection", column = "settlement_direction"),
            @Result(property = "targetCountry", column = "target_country"),
            @Result(property = "settlementDate", column = "settlement_date"),
            @Result(property = "isAbnormal", column = "is_abnormal"),
            @Result(property = "abnormalReason", column = "abnormal_reason"),
            @Result(property = "creditScore", column = "credit_score"),
            @Result(property = "dataSource", column = "data_source"),
            @Result(property = "createTime", column = "create_time")
    })
    List<SettlementCredit> selectByCreditCode(@Param("creditCode") String creditCode);

    @Select("SELECT AVG(credit_score) FROM t_settlement_credit WHERE credit_code = #{creditCode}")
    Double selectAverageCreditScoreByCreditCode(@Param("creditCode") String creditCode);

    @Select("SELECT * FROM t_settlement_credit WHERE is_abnormal = 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "creditCode", column = "credit_code"),
            @Result(property = "enterpriseName", column = "enterprise_name"),
            @Result(property = "settlementType", column = "settlement_type"),
            @Result(property = "settlementCurrency", column = "settlement_currency"),
            @Result(property = "settlementAmount", column = "settlement_amount"),
            @Result(property = "settlementDirection", column = "settlement_direction"),
            @Result(property = "targetCountry", column = "target_country"),
            @Result(property = "settlementDate", column = "settlement_date"),
            @Result(property = "isAbnormal", column = "is_abnormal"),
            @Result(property = "abnormalReason", column = "abnormal_reason"),
            @Result(property = "creditScore", column = "credit_score"),
            @Result(property = "dataSource", column = "data_source"),
            @Result(property = "createTime", column = "create_time")
    })
    List<SettlementCredit> selectAbnormalRecords();

    @Insert("INSERT INTO t_settlement_credit " +
            "(credit_code, enterprise_name, settlement_type, settlement_currency, " +
            "settlement_amount, settlement_direction, target_country, settlement_date, " +
            "is_abnormal, abnormal_reason, credit_score, data_source, create_time) " +
            "VALUES " +
            "(#{creditCode}, #{enterpriseName}, #{settlementType}, #{settlementCurrency}, " +
            "#{settlementAmount}, #{settlementDirection}, #{targetCountry}, #{settlementDate}, " +
            "#{isAbnormal}, #{abnormalReason}, #{creditScore}, #{dataSource}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SettlementCredit settlementCredit);

    @Select("SELECT * FROM t_settlement_credit ORDER BY settlement_date DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "creditCode", column = "credit_code"),
            @Result(property = "enterpriseName", column = "enterprise_name"),
            @Result(property = "settlementType", column = "settlement_type"),
            @Result(property = "settlementCurrency", column = "settlement_currency"),
            @Result(property = "settlementAmount", column = "settlement_amount"),
            @Result(property = "settlementDirection", column = "settlement_direction"),
            @Result(property = "targetCountry", column = "target_country"),
            @Result(property = "settlementDate", column = "settlement_date"),
            @Result(property = "isAbnormal", column = "is_abnormal"),
            @Result(property = "abnormalReason", column = "abnormal_reason"),
            @Result(property = "creditScore", column = "credit_score"),
            @Result(property = "dataSource", column = "data_source"),
            @Result(property = "createTime", column = "create_time")
    })
    List<SettlementCredit> findAll(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM t_settlement_credit")
    int countAll();

    @Select("SELECT DATE_FORMAT(settlement_date, '%Y-%m') AS month, " +
            "SUM(CASE WHEN settlement_direction = '收入' THEN settlement_amount ELSE 0 END) AS exportAmount, " +
            "SUM(CASE WHEN settlement_direction = '支出' THEN settlement_amount ELSE 0 END) AS importAmount " +
            "FROM t_settlement_credit GROUP BY month ORDER BY month")
    List<Map<String, Object>> monthlyTrend();

    @Select("SELECT DISTINCT target_country FROM t_settlement_credit WHERE credit_code = #{creditCode}")
    List<Map<String,Object>> uniqueCounterCountries(@Param("creditCode") String creditCode);

    @Select("SELECT settlement_amount AS amount, settlement_date, target_country " +
            "FROM t_settlement_credit WHERE credit_code = #{creditCode} " +
            "AND settlement_date >= DATE_SUB(NOW(), INTERVAL #{months} MONTH) " +
            "ORDER BY settlement_date DESC")
    List<Map<String,Object>> recentSettlements(@Param("creditCode") String creditCode, @Param("months") int months);
}
