package com.risk.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface WarningSubscriptionMapper {
    @Insert("INSERT INTO t_warning_subscription(open_id, credit_code, create_time) VALUES(#{openId}, #{creditCode}, NOW())")
    int insert(@Param("openId") String openId, @Param("creditCode") String creditCode);

    @Delete("DELETE FROM t_warning_subscription WHERE open_id = #{openId} AND credit_code = #{creditCode}")
    int delete(@Param("openId") String openId, @Param("creditCode") String creditCode);

    @Select("SELECT COUNT(*) FROM t_warning_subscription WHERE open_id = #{openId} AND credit_code = #{creditCode}")
    int exists(@Param("openId") String openId, @Param("creditCode") String creditCode);

    @Select("SELECT s.credit_code AS enterpriseId, " +
            "COALESCE(e.enterprise_name, s.credit_code) AS enterpriseName " +
            "FROM t_warning_subscription s " +
            "LEFT JOIN t_enterprise e ON e.credit_code = s.credit_code " +
            "WHERE s.open_id = #{openId} ORDER BY s.create_time DESC")
    List<Map<String, Object>> findSubscribedEnterprises(@Param("openId") String openId);
}
