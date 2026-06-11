#FROM egovio/alpine-maven-builder-jdk-8:gcp AS build
FROM maven:3.8.6-openjdk-8-slim AS build
ARG WORK_DIR
WORKDIR /app

# copy the project files

COPY ${WORK_DIR} ./${WORK_DIR}

    
RUN cd ${WORK_DIR} && \
    mvn clean package -DskipTests
#    -Dmaven.wagon.http.ssl.insecure=true \
 #   -Dmaven.wagon.http.ssl.allowall=true
                                 

# Create runtime image

#FROM egovio/wildfly:1-helm-fin-e6312078
FROM upyogio/wildfly:1-ed
COPY --from=build /app/egov/egov-ear/target/*.ear /opt/jboss/wildfly/standalone/deployments/
USER jboss

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-Ddb.migration.enabled=false"]
