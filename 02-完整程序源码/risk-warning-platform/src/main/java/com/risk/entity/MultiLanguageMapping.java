package com.risk.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 多语种企业名映射
 */
@Data
@Alias("MultiLanguageMapping")
public class MultiLanguageMapping {
    private Long id;
    private String creditCode;
    private String nameCn;          // 中文名
    private String nameEn;          // 英文名
    private String nameVn;          // 越南语
    private String nameTh;          // 泰语
    private String nameMy;          // 马来语
    private String nameId;          // 印尼语
    private String nameKh;          // 柬埔寨语
    private String sourceCountry;   // 来源国
}
