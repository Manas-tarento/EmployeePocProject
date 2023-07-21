FROM openjdk:11
ADD target/postgres-poc.jar postgres-poc.jar
ENTRYPOINT ["java","-jar","/postgres-poc.jar"]