package com.risk.mapper;

import com.risk.entity.DatasourceConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DatasourceConfigMapper {

    @Select("SELECT * FROM t_datasource_config ORDER BY id ASC")
    List<DatasourceConfig> findAll();

    @Select("SELECT * FROM t_datasource_config WHERE type = #{type} LIMIT 1")
    DatasourceConfig findByType(@Param("type") String type);

    @Insert("INSERT INTO t_datasource_config(type, name, url, api_key, status, remark) " +
            "VALUES(#{type}, #{name}, #{url}, #{apiKey}, #{status}, #{remark}) " +
            "ON DUPLICATE KEY UPDATE name=VALUES(name), url=VALUES(url), api_key=VALUES(api_key), " +
            "status=VALUES(status), remark=VALUES(remark), update_time=NOW()")
    int upsert(DatasourceConfig config);
}
