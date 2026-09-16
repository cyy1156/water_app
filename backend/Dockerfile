# ========= 第一阶段：使用 Maven 编译打包 =========
FROM maven:3.9.6-eclipse-temurin-8 AS build

# 工作目录
WORKDIR /app

# 先拷贝 pom，加快依赖缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再拷贝源码
COPY src ./src

# 打包（跳过测试）
RUN mvn -B -Dmaven.test.skip=true clean package

# ========= 第二阶段：使用精简 JRE 运行 =========
FROM eclipse-temurin:8-jre

WORKDIR /app

# 复制打包好的 jar 到运行镜像里，命名为 app.jar
COPY --from=build /app/target/*.jar /app/app.jar

# 容器内应用监听端口（Spring Boot 默认 8080）
EXPOSE 8080

# 时区（可选）
ENV TZ=Asia/Shanghai

# 如果你在 application.yml 里已经配置了端口和 context-path，可以不用额外参数
ENTRYPOINT ["java","-jar","/app/app.jar"]