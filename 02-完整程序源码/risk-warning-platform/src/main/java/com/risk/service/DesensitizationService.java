package com.risk.service;

import org.springframework.stereotype.Service;

/**
 * 分层脱敏融合服务 —— 技术架构创新(五)「多源公共数据安全融合脱敏框架」的核心实现。
 *
 * <p>在对接广西政务开放数据、东盟公开商事数据的跨域关联场景下，对敏感字段按数据分级
 * 做差异化脱敏，确保「不泄露企业/个人敏感隐私」前提下完成跨境数据关联与价值释放。</p>
 *
 * <p>脱敏分级（对应赛题“分层脱敏”要求）：
 * <ul>
 *   <li>{@link Level#PUBLIC} 公开级：完整展示（企业名称、公开术语、公开评级）</li>
 *   <li>{@link Level#INTERNAL} 内部级：姓名保留首字打码（张三 → 张**）</li>
 *   <li>{@link Level#CONFIDENTIAL} 机密级：企业名/证件强掩码（广西贸公司 → 广西**司）</li>
 * </ul>
 * </p>
 *
 * <p>所有对外 API 在返回前统一经本服务处理，并在响应体标注 {@code desensitized:true}，
 * 形成“可审计、可追溯、可分级”的脱敏闭环。</p>
 */
@Service
public class DesensitizationService {

    /** 脱敏分级 */
    public enum Level {
        /** 公开级：不脱敏 */
        PUBLIC,
        /** 内部级：姓名/法人弱掩码 */
        INTERNAL,
        /** 机密级：企业名/证件强掩码 */
        CONFIDENTIAL
    }

    /**
     * 姓名 / 法人脱敏：保留首字，其余以 * 代替（张三 → 张**）。
     * 单字或空值原样返回，避免误伤。
     */
    public String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + repeatAsterisk(name.length() - 1);
    }

    /**
     * 企业名称脱敏：保留前 2 字与末 1 字，中间以 * 代替（广西贸公司 → 广西**司）。
     * 长度 ≤ 3 时退化为 {@link #maskName(String)}。
     */
    public String maskEnterprise(String name) {
        if (name == null || name.length() <= 3) {
            return maskName(name);
        }
        String head = name.substring(0, 2);
        String tail = name.substring(name.length() - 1);
        return head + repeatAsterisk(name.length() - 3) + tail;
    }

    /**
     * 手机号脱敏：保留前 3 后 4（138****1234）。非 11 位退回姓名脱敏策略。
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return maskName(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 身份证号脱敏：保留前 6 后 4，中间以 * 代替（4501******1234）。
     * 长度不足 10 时退回姓名脱敏策略，避免暴露短号码。
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return maskName(idCard);
        }
        String head = idCard.substring(0, 6);
        String tail = idCard.substring(idCard.length() - 4);
        return head + repeatAsterisk(idCard.length() - 10) + tail;
    }

    /**
     * 通用分级脱敏入口：按 {@link Level} 选择策略，便于在跨域关联结果中统一调用。
     *
     * @param value 原始值（可空）
     * @param level 脱敏分级
     * @return 脱敏后的值；空值原样返回
     */
    public String desensitize(String value, Level level) {
        if (value == null) {
            return null;
        }
        switch (level) {
            case PUBLIC:
                return value;
            case INTERNAL:
                return maskName(value);
            case CONFIDENTIAL:
                return maskEnterprise(value);
            default:
                return value;
        }
    }

    /** 生成 n 个星号（n ≤ 0 返回空串）。 */
    private String repeatAsterisk(int n) {
        if (n <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
