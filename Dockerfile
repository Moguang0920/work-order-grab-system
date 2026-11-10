# 多阶段构建：第一阶段用 Maven + JDK 编译，第二阶段只带 JRE，镜像更小
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先只拷 pom，让依赖层可以被缓存（改了代码也不会重新下依赖）
COPY nameless/pom.xml ./
RUN mvn -B -q dependency:go-offline

# 再拷源码编译
COPY nameless/src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
