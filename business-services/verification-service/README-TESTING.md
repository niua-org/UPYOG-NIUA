# verification-service testing

```bash
export JAVA_HOME=/tmp/jdk-17 PATH=$JAVA_HOME/bin:/tmp/apache-maven-3.9.6/bin:$PATH
mvn -f business-services/test-support/pom.xml install -DskipTests
mvn -f business-services/verification-service/pom.xml test verify
```

Unit tests cover `CommonServiceImpl`, `UserService`, mappers, util, and validation without Spring context. Legacy `@Disabled` integration tests remain unchanged.
