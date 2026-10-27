# 基于Eclipse Temurin JDK 21构建
FROM eclipse-temurin:21-jre-alpine

# 设置时区为上海
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk add --no-cache font-noto-cjk

# 工作目录
WORKDIR /app

# 复制JAR包（由Maven构建生成）
COPY target/risk-warning-platform-1.0.0.jar app.jar

# 复制Docker环境配置文件
COPY src/main/resources/application-docker.yml /app/application.yml

# 暴露端口
EXPOSE 8080

# JVM参数 + 启动命令
ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-Dfile.encoding=UTF-8", \
    "-jar", \
    "app.jar", \
    "--spring.config.location=/app/application.yml"]
