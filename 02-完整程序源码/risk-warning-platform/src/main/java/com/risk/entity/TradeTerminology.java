package com.risk.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 跨境商事术语库
 */
@Data
@Alias("TradeTerminology")
public class TradeTerminology {
    private Long id;
    @JsonProperty("termZh")
    private String termCn;          // 中文术语
    @JsonProperty("termEn")
    private String termEn;          // 英文
    @JsonProperty("termVi")
    private String termVn;          // 越南语
    @JsonProperty("termTh")
    private String termTh;          // 泰语
    @JsonProperty("termMy")
    private String termMy;          // 马来语（Bahasa Melayu）
    @JsonProperty("termIn")
    private String termIn;          // 印尼语（Bahasa Indonesia）
    @JsonProperty("termKm")
    private String termKm;          // 柬埔寨语（Khmer）
    private String category;        // 分类：报关/税务/物流/支付/法律
    @JsonProperty("hsCode")
    private String hsCode;          // HS编码（可选）
    private String description;     // 说明
    @JsonProperty("dataSource")
    private String source;          // 数据来源
}
