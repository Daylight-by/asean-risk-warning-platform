package com.risk.entity;

import lombok.Data;

@Data
public class PortGeo {
    private Long id;
    private String portName;
    private String portLevel;
    private String portType;
    private Double longitude;
    private Double latitude;
    private String borderCountry;
    private String dataSource;
}
