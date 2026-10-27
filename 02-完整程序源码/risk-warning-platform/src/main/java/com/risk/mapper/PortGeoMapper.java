package com.risk.mapper;

import com.risk.entity.PortGeo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PortGeoMapper {

    @Select("SELECT * FROM t_port_geo ORDER BY port_name")
    List<PortGeo> findAll();

    @Select("SELECT * FROM t_port_geo WHERE port_name = #{portName}")
    PortGeo findByPortName(String portName);
}
