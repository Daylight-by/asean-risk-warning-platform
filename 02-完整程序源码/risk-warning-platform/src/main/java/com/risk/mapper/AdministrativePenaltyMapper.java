package com.risk.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AdministrativePenaltyMapper {
    @Select("SELECT COUNT(*) FROM t_administrative_penalty WHERE credit_code = #{creditCode}")
    int countByCreditCode(@Param("creditCode") String creditCode);
}
