# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY settings.xml /usr/share/maven/ref/settings.xml
COPY pom.xml .
RUN mvn -s /usr/share/maven/ref/settings.xml -B dependency:resolve dependency:resolve-plugins
COPY src ./src
RUN mvn -s /usr/share/maven/ref/settings.xml -B package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
