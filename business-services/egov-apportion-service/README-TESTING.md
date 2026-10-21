# egov-apportion-service testing

```bash
export JAVA_HOME=/tmp/jdk-17 PATH=$JAVA_HOME/bin:/tmp/apache-maven-3.9.6/bin:$PATH
mvn -f business-services/test-support/pom.xml install -DskipTests
mvn -f business-services/egov-apportion-service/pom.xml test verify
```

JaCoCo gates target `org.egov.service` and `org.egov.service.apportions`. Flyway ITs skip when Docker is unavailable.
