package com.risk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DatasourceConfig {
    private Long id;
    private String type;
    private String name;
    private String url;
    private String apiKey;
    private String status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
