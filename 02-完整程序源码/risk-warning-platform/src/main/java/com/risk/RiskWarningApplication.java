package com.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 东盟跨境中小商户智能风控预警平台 - 启动类
 */
@SpringBootApplication
public class RiskWarningApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskWarningApplication.class, args);
        System.out.println("==========================================");
        System.out.println("  东盟跨境中小商户智能风控预警平台 启动成功");
        System.out.println("  Data Element × Cross-Border Trade");
        System.out.println("==========================================");
    }
}
