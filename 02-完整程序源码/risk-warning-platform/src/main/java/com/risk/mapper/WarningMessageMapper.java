package com.risk.mapper;

import com.risk.entity.WarningMessage;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface WarningMessageMapper {
    @Insert("INSERT INTO t_warning_message(credit_code, enterprise_name, warning_type, warning_level, warning_content, data_source, create_time) " +
            "VALUES(#{creditCode}, #{enterpriseName}, #{warningType}, #{warningLevel}, #{content}, #{dataSource}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WarningMessage msg);

    @Select("SELECT * FROM t_warning_message ORDER BY create_time DESC LIMIT #{limit}")
    @Results(id = "warningMsgMap", value = {
        @Result(column = "warning_content", property = "content"),
        @Result(column = "warning_level", property = "warningLevel"),
        @Result(column = "warning_type", property = "warningType"),
        @Result(column = "credit_code", property = "creditCode"),
        @Result(column = "enterprise_name", property = "enterpriseName"),
        @Result(column = "data_source", property = "dataSource"),
        @Result(column = "is_read", property = "isRead"),
        @Result(column = "status", property = "status"),
        @Result(column = "handler", property = "handler"),
        @Result(column = "handle_time", property = "handleTime"),
        @Result(column = "handle_result", property = "handleResult"),
        @Result(column = "create_time", property = "createTime")
    })
    List<WarningMessage> findLatest(@Param("limit") int limit);

    @Select("SELECT * FROM t_warning_message WHERE credit_code IN " +
            "(SELECT credit_code FROM t_warning_subscription WHERE open_id = #{openId}) " +
            "ORDER BY create_time DESC LIMIT 50")
    @ResultMap("warningMsgMap")
    List<WarningMessage> findByOpenId(@Param("openId") String openId);

    @Select("<script>" +
            "SELECT * FROM t_warning_message WHERE 1=1 " +
            "<if test=\"type != null and type != ''\"> AND warning_type = #{type}</if>" +
            "<if test=\"level != null and level != ''\"> AND warning_level = #{level}</if>" +
            "<if test=\"status != null and status != ''\"> AND status = #{status}</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND create_time &lt; DATE_ADD(CONCAT(#{endDate}, ' 00:00:00'), INTERVAL 1 DAY)</if>" +
            "<choose>" +
            "  <when test=\"sortField == 'statusPriority'\">" +
            "    ORDER BY CASE status WHEN '未处理' THEN 0 WHEN '处理中' THEN 1 WHEN '已处理' THEN 2 WHEN '已关闭' THEN 3 ELSE 9 END " +
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
            " LIMIT #{offset}, #{limit}" +
            "</script>")
    @ResultMap("warningMsgMap")
    List<WarningMessage> findByPage(@Param("offset") int offset, @Param("limit") int limit,
                                    @Param("sortField") String sortField, @Param("sortOrder") String sortOrder,
                                    @Param("type") String type, @Param("level") String level,
                                    @Param("status") String status, @Param("startDate") String startDate,
                                    @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_warning_message WHERE 1=1 " +
            "<if test=\"type != null and type != ''\"> AND warning_type = #{type}</if>" +
            "<if test=\"level != null and level != ''\"> AND warning_level = #{level}</if>" +
            "<if test=\"status != null and status != ''\"> AND status = #{status}</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND create_time &lt; DATE_ADD(CONCAT(#{endDate}, ' 00:00:00'), INTERVAL 1 DAY)</if>" +
            "</script>")
    int countFiltered(@Param("type") String type, @Param("level") String level,
                      @Param("status") String status, @Param("startDate") String startDate,
                      @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM t_warning_message")
    int countAll();

    @Select("SELECT COUNT(*) FROM t_warning_message WHERE create_time >= DATE_FORMAT(NOW(),'%Y-%m-01')")
    int countThisMonth();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as cnt " +
            "FROM t_warning_message WHERE create_time >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month ASC")
    List<Map<String,Object>> monthlyTrend();

    /** 局部更新：仅切换处理状态，避免全字段替换触发 NOT NULL 约束 */
    @Update("UPDATE t_warning_message SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 处理闭环：标记为已处理，写处理人/说明/时间（原子单次 UPDATE） */
    @Update("UPDATE t_warning_message SET status = '已处理', handler = #{handler}, " +
            "handle_result = #{handleResult}, handle_time = NOW() WHERE id = #{id}")
    int processWarning(@Param("id") Long id,
                       @Param("handler") String handler,
                       @Param("handleResult") String handleResult);
}
