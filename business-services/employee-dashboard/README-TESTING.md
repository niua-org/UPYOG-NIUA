# employee-dashboard testing

```bash
export JAVA_HOME=/tmp/jdk-17 PATH=$JAVA_HOME/bin:/tmp/apache-maven-3.9.6/bin:$PATH
mvn -f business-services/test-support/pom.xml install -DskipTests
mvn -f business-services/employee-dashboard/pom.xml test verify
```

Service and util packages are covered with Mockito-based unit tests and `@WebMvcTest`-style controller smoke tests.
