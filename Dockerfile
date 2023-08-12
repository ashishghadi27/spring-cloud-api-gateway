FROM openjdk:17-alpine
EXPOSE 8090
ADD target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]