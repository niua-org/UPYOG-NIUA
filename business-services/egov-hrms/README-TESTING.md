# egov-hrms testing

```bash
export JAVA_HOME=/tmp/jdk-17 PATH=$JAVA_HOME/bin:/tmp/apache-maven-3.9.6/bin:$PATH
mvn -f business-services/test-support/pom.xml install -DskipTests
mvn -f business-services/egov-hrms/pom.xml test verify
```

Unit tests use JUnit 5, Mockito, and AssertJ (no `@SpringBootTest`). JaCoCo gates apply to `org.egov.hrms.service`, validation/utils, and `web.validator` packages.
