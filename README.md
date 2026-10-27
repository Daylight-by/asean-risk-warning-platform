# 东盟跨境中小商户智能风控预警平台

本科高职组 · 26 号 · 初赛完整提交包（2026-08-24）

面向中国—东盟跨境中小商户的可解释风险评分与事前预警原型：整合海关吞吐、口岸、贸易统计、
政策目录等公开数据，基于「可解释评分卡 + 国别规则配置 + 事前预警特征」输出企业合规风险辅助判断，
配套微信小程序（商户/查询端）、Web 政务监管端与 Spring Boot 后端。

> 本仓库为赛事原型演示。系统输出为风险辅助判断，不构成行政处罚、海关裁定、司法判决或金融授信结论；
> 页面与接口中的企业、交易数据均为 DEMO 样例，不代表真实政务数据或真实征信结论。

## 目录结构

```
.
├── 01-参赛作品与方案说明书/        # 方案说明书 PDF
├── 02-完整程序源码/
│   ├── risk-warning-platform/      # Spring Boot 后端（Java 21 / MyBatis / MySQL / Redis）
│   ├── web-admin/                  # Web 政务监管端（Vue 2 + Element UI + ECharts）
│   ├── miniprogram/                # 微信小程序端（17 个页面，含 AI 助手）
│   └── sql/                        # 补充数据脚本（初始化脚本位于后端 sql/ 目录）
├── 03-作品使用数据集/             # 海关/口岸/贸易统计/政策目录等公开原始数据
├── 05-模型与数据说明/              # 风控模型说明书、回测与误报分析、数据可信说明
└── 06-测试报告与运行说明/          # 测试报告 PDF、运行说明
```

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.3.5、Java 21、MyBatis 3.0.3、MySQL 8、Redis、Spring Security Crypto（仅密码哈希） |
| 监管端 | Vue 2.7、Element UI 2.15、ECharts 5、Vue Router、Axios |
| 小程序 | 原生微信小程序（WXML/WXSS/JS），可经本地存储配置后端地址 |
| 部署 | Docker Compose（MySQL + Redis + 应用） |

## 快速启动

环境要求：JDK 21、Maven Wrapper（已随源码提供）、MySQL 8.x、Node.js 16+、微信开发者工具（仅小程序需要）。

1. 初始化数据库：先执行 `02-完整程序源码/risk-warning-platform/sql/init.sql`，再按需执行同目录种子脚本。
2. 启动后端（`risk-warning-platform/` 目录）：
   ```powershell
   .\mvnw.cmd spring-boot:run
   # 或先打包再运行
   .\mvnw.cmd -q package -DskipTests
   java -jar target\risk-warning-platform-1.0.0.jar
   ```
   后端地址 `http://127.0.0.1:8080`，演示账号 `admin` / `123456`（正式部署请用 `APP_AUTH_ADMIN_HASH` 注入 BCrypt 哈希替换）。
3. 启动监管端（`web-admin/` 目录）：
   ```powershell
   npm install
   npm run serve
   ```
   访问 `http://127.0.0.1:3000`。
4. 小程序：用微信开发者工具导入 `miniprogram/`，后端地址填 `http://127.0.0.1:8080/api`；真机调试在本地存储配置 `apiBaseURL` 为局域网地址。
5. Docker 一键启动（`risk-warning-platform/` 目录）：
   ```powershell
   docker compose up --build
   ```

更详细的步骤见 `06-测试报告与运行说明/运行说明.md`。

## 模型说明

综合得分为「安全/合规得分」，越高代表可观测风险越低：0–40 高风险、41–70 中风险、71–100 低风险。
评分由基础资质(20%)、经营合规(30%)、口岸通关(25%)、贸易活跃(15%)、综合调整(10%)加权并扣除风险项，
阈值与国别权重持久化在数据库、可由监管端配置。详见 `05-模型与数据说明/风控模型说明书.md`。

## 安全说明

- 仓库内数据库/Redis 密码、Token 密钥均为本地演示默认值并支持环境变量覆盖（`DB_PASSWORD`、`APP_AUTH_SECRET`、`APP_AUTH_ADMIN_HASH` 等），无生产凭据。
- 外部海关/税务/工商 API 的 `url`、`key` 均留空，待接入授权端点。
- AI 助手默认 `mock` 模式（基于本地业务数据生成回复），不内置第三方模型密钥。
