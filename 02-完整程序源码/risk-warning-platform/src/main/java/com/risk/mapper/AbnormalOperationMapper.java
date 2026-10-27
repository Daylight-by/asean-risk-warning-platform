package com.risk.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AbnormalOperationMapper {

    @Select("SELECT COUNT(*) FROM t_abnormal_operation WHERE credit_code = #{creditCode}")
    int countByCreditCode(@Param("creditCode") String creditCode);

    @Select("SELECT COUNT(*) FROM t_abnormal_operation WHERE credit_code = #{creditCode} " +
            "AND list_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR)")
    int countRecentByCreditCode(@Param("creditCode") String creditCode);
}
