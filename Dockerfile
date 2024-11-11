FROM openjdk:17.0.2
EXPOSE 8080:8080
ARG TASK_SERVICE_URL
ARG USER_SERVICE_URL
ENV TASK_SERVICE_URL=$TASK_SERVICE_URL \
    USER_SERVICE_URL=$USER_SERVICE_URL
RUN mkdir /app
COPY ./build/libs/*-all.jar /app/task-management-fe.jar
ENTRYPOINT ["java", "-jar", "/app/task-management-fe.jar"]