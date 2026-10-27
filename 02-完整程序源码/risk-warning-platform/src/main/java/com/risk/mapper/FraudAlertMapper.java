package com.risk.mapper;

import com.risk.entity.FraudAlert;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FraudAlertMapper {

    /**
     * 共享结果映射：把 @Results 放在一个标准方法上（用 id 命名），
     * 其他 SELECT 方法用 @ResultMap("fraudAlertMap") 复用，避免新增字段时 N 处同步。
     * 注意：MyBatis 不支持在内部类上声明 @Results，必须挂在方法上。
     */
    @Select("<script>" +
            "SELECT * FROM t_fraud_alert " +
            "<choose>" +
            "  <when test=\"sortField == 'statusPriority'\">" +
            "    ORDER BY CASE is_confirmed WHEN 0 THEN 0 WHEN 1 THEN 1 WHEN 2 THEN 2 ELSE 9 END " +
            "    <choose><when test=\"sortOrder == 'asc'\">ASC</when><otherwise>DESC</otherwise></choose>, create_time DESC" +
            "  </when>" +
            "  <when test=\"sortField == 'handleTime'\">" +
            "    ORDER BY handle_time " +
            "    <choose><when test=\"sortOrder == 'asc'\">ASC</when><otherwise>DESC</otherwise></choose>" +
            "  </when>" +
            "  <otherwise>" +
            "    ORDER BY create_time " +
            "    <choose><when test=\"sortOrder == 'asc'\">ASC</when><otherwise>DESC</otherwise></choose>" +
            "  </otherwise>" +
            "</choose>" +
            " LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results(id = "fraudAlertMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "creditCode", column = "credit_code"),
            @Result(property = "enterpriseName", column = "enterprise_name"),
            @Result(property = "fraudType", column = "fraud_type"),
            @Result(property = "riskProbability", column = "risk_probability"),
            @Result(property = "riskLevel", column = "risk_level"),
            @Result(property = "alertContent", column = "alert_content"),
            @Result(property = "modelFeatures", column = "model_features"),
            @Result(property = "predictionPeriod", column = "prediction_period"),
            @Result(property = "isConfirmed", column = "is_confirmed"),
            @Result(property = "handler", column = "handler"),
            @Result(property = "handleResult", column = "handle_result"),
            @Result(property = "handleTime", column = "handle_time"),
            @Result(property = "handleAction", column = "handle_action"),
            @Result(property = "dataSource", column = "data_source"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    List<FraudAlert> findAll(@Param("offset") int offset, @Param("limit") int limit,
                             @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("SELECT * FROM t_fraud_alert WHERE credit_code = #{creditCode}")
    @ResultMap("fraudAlertMap")
    List<FraudAlert> selectByCreditCode(@Param("creditCode") String creditCode);

    @Select("SELECT * FROM t_fraud_alert WHERE risk_level = #{riskLevel}")
    @ResultMap("fraudAlertMap")
    List<FraudAlert> selectByRiskLevel(@Param("riskLevel") String riskLevel);

    @Select("SELECT COUNT(*) FROM t_fraud_alert WHERE is_confirmed = 0")
    int countUnconfirmed();

    @Select("SELECT COUNT(*) FROM t_fraud_alert")
    int countAll();

    @Update("UPDATE t_fraud_alert SET is_confirmed = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE t_fraud_alert SET is_confirmed = #{isConfirmed}, handler = #{handler}, " +
            "handle_result = #{handleResult}, update_time = #{updateTime} WHERE id = #{id}")
    int updateConfirmation(@Param("id") Long id,
                           @Param("isConfirmed") Integer isConfirmed,
                           @Param("handler") String handler,
                           @Param("handleResult") String handleResult,
                           @Param("updateTime") LocalDateTime updateTime);

    /** 处置闭环：标记为已确认(1)，写处理人/动作/意见/时间（原子单次 UPDATE） */
    @Update("UPDATE t_fraud_alert SET is_confirmed = 1, handler = #{handler}, " +
            "handle_action = #{handleAction}, handle_result = #{handleResult}, " +
            "handle_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int processAlert(@Param("id") Long id,
                     @Param("handler") String handler,
                     @Param("handleAction") String handleAction,
                     @Param("handleResult") String handleResult);

    /** 处置统计：今日新增 / 待核实 / 已处理 / 处理率，COUNT + SUM(CASE WHEN) 单次聚合 */
    @Select("SELECT " +
            "  SUM(CASE WHEN DATE(create_time) = CURDATE() THEN 1 ELSE 0 END) AS todayNew, " +
            "  SUM(CASE WHEN is_confirmed = 0 THEN 1 ELSE 0 END) AS pending, " +
            "  SUM(CASE WHEN is_confirmed = 1 THEN 1 ELSE 0 END) AS processed, " +
            "  COUNT(*) AS total " +
            "FROM t_fraud_alert")
    Map<String, Object> getProcessStats();
}
