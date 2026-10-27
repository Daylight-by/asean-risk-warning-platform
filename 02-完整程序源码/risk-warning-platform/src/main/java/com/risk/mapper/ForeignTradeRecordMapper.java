package com.risk.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ForeignTradeRecordMapper {
    @Select("SELECT COUNT(*) FROM t_foreign_trade_record WHERE credit_code = #{creditCode} AND record_type = '外贸经营者'")
    int hasForeignTradeRecord(@Param("creditCode") String creditCode);
    @Select("SELECT COUNT(*) FROM t_foreign_trade_record WHERE credit_code = #{creditCode} AND record_type = '跨境电商'")
    int hasCrossBorderEcommerceRecord(@Param("creditCode") String creditCode);
}
