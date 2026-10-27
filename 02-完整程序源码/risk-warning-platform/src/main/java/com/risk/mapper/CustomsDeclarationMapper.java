package com.risk.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustomsDeclarationMapper {

    @Select("SELECT COUNT(*) FROM t_customs_declaration WHERE credit_code = #{creditCode} AND is_violation = 1")
    int countViolations(@Param("creditCode") String creditCode);

    @Select("SELECT COUNT(*) FROM t_customs_declaration WHERE credit_code = #{creditCode} " +
            "AND declaration_date >= DATE_SUB(NOW(), INTERVAL 12 MONTH)")
    int countRecent12Month(@Param("creditCode") String creditCode);

    @Select("SELECT IFNULL(COUNT(*),0) FROM t_customs_declaration WHERE credit_code = #{creditCode}")
    int countAll(@Param("creditCode") String creditCode);

    @Select("SELECT IFNULL(AVG(CASE WHEN inspection_result='查验异常' THEN 1.0 ELSE 0.0 END),0) " +
            "FROM t_customs_declaration WHERE credit_code = #{creditCode}")
    double avgInspectionAbnormalRate(@Param("creditCode") String creditCode);

    @Select("SELECT IFNULL(AVG(clearance_duration),0) FROM t_customs_declaration WHERE credit_code = #{creditCode}")
    double avgClearanceDuration(@Param("creditCode") String creditCode);

    @Select("SELECT COUNT(DISTINCT DATE_FORMAT(declaration_date,'%Y-%m')) " +
            "FROM t_customs_declaration WHERE credit_code = #{creditCode} " +
            "AND declaration_date >= DATE_SUB(NOW(), INTERVAL 12 MONTH)")
    int countContinuousMonths(@Param("creditCode") String creditCode);

    @Select("SELECT COUNT(DISTINCT goods_category) FROM t_customs_declaration " +
            "WHERE credit_code = #{creditCode}")
    int countGoodsCategory(@Param("creditCode") String creditCode);

    @Select("SELECT IFNULL(COUNT(DISTINCT credit_code),0) FROM t_customs_declaration WHERE port_name = #{portName}")
    int countAllByPort(@Param("portName") String portName);

    @Select("SELECT DATE_FORMAT(declaration_date, '%Y-%m') AS month, " +
            "IFNULL(SUM(declared_value), 0) AS totalValue " +
            "FROM t_customs_declaration WHERE credit_code = #{creditCode} " +
            "GROUP BY month ORDER BY month")
    List<Map<String,Object>> monthlyDeclarationValue(@Param("creditCode") String creditCode);

    /** 幂等判重：用于外部数据同步时识别已存在的报关记录 */
    @Select("SELECT COUNT(*) FROM t_customs_declaration WHERE credit_code = #{creditCode} AND port_name = #{portName} AND declaration_date = #{declarationDate} AND goods_category = #{goodsCategory}")
    int countByUnique(@Param("creditCode") String creditCode, @Param("portName") String portName,
                      @Param("declarationDate") String declarationDate, @Param("goodsCategory") String goodsCategory);

    /** 外部数据同步落库：插入一条报关记录（字段缺失由服务层兜底为默认值） */
    @Insert("INSERT INTO t_customs_declaration(credit_code, enterprise_name, port_name, declaration_type, declaration_date, goods_category, is_violation, inspection_result, clearance_duration, data_source) " +
            "VALUES(#{creditCode}, #{enterpriseName}, #{portName}, #{declarationType}, #{declarationDate}, #{goodsCategory}, #{isViolation}, #{inspectionResult}, #{clearanceDuration}, #{dataSource})")
    int insertDeclaration(Map<String, Object> declaration);
}
