#!/usr/bin/env python3
"""Generate test scaffolding per municipal-services Maven module."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SMOKE_ROOT = ROOT / "core-smoke-tests"
METADATA = Path(__file__).with_name("modules-test-metadata.json")

TEST_DEPS = """
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.wiremock</groupId>
            <artifactId>wiremock-standalone</artifactId>
            <version>3.3.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>1.2.1</version>
            <scope>test</scope>
        </dependency>
"""

PLUGINS_BLOCK = """
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>**/*IT.java</exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <includes>
                        <include>**/*IT.java</include>
                    </includes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>coverage-check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>PACKAGE</element>
                                    <includes>
                                        <include>{jacoco_pkg}</include>
                                    </includes>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>${{jacoco.line.coverage}}</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
"""

OWASP_PROFILE = """
    <profiles>
        <profile>
            <id>owasp</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.owasp</groupId>
                        <artifactId>dependency-check-maven</artifactId>
                        <version>9.0.9</version>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>check</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
"""


def smoke_class_name(module: str) -> str:
    parts = module.replace("_", "-").split("-")
    return "".join(p.capitalize() for p in parts) + "SmokeTest"


def smoke_java_package(module: str) -> str:
    short = module.replace("-", "")
    return f"org.niua.smoke.{short}"


def jacoco_package(test_class: str) -> str:
    pkg = ".".join(test_class.split(".")[:-1])
    return pkg


def archunit_package(base: str) -> str:
    return base


def write_archunit(module_dir: Path, base_package: str) -> None:
    pkg = base_package
    if pkg == "org.egov":
        # use module-specific subfolder
        sub = module_dir.name.replace("-", "")
        test_pkg = f"org.egov.architecture.{sub}"
    else:
        test_pkg = pkg + ".architecture"
    path = module_dir / "src/test/java" / Path(*test_pkg.split(".")) / "LayeredArchitectureTest.java"
    if path.exists():
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    analyze = base_package if base_package != "upyog" else "upyog"
    content = f"""package {test_pkg};

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "{analyze}", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {{

    @ArchTest
    static final ArchRule controllersDoNotAccessRepositories = noClasses()
            .that().resideInAnyPackage("..web..", "..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");
}}
"""
    path.write_text(content)


def sanitize_html_test(test_class: str) -> str:
    pkg = ".".join(test_class.split(".")[:-1])
    simple = test_class.split(".")[-1]
    return f"""package {pkg};

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class {simple}Test {{

    private {simple} validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {{
        validator = new {simple}();
    }}

    @Test
    @DisplayName("isValid_whenNull_returnsTrue")
    void isValid_whenNull_returnsTrue() {{
        assertThat(validator.isValid(null, context)).isTrue();
    }}

    @Test
    @DisplayName("isValid_whenPlainText_returnsTrue")
    void isValid_whenPlainText_returnsTrue() {{
        assertThat(validator.isValid("hello", context)).isTrue();
    }}

    @Test
    @DisplayName("isValid_whenScriptTag_returnsFalse")
    void isValid_whenScriptTag_returnsFalse() {{
        assertThat(validator.isValid("<script>alert(1)</script>", context)).isFalse();
    }}
}}
"""


def response_info_test(test_class: str) -> str:
    pkg = ".".join(test_class.split(".")[:-1])
    simple = test_class.split(".")[-1]
    return f"""package {pkg};

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class {simple}Test {{

    private final {simple} factory = new {simple}();

    @Test
    @DisplayName("createResponseInfoFromRequestInfo_whenSuccess_setsSuccessfulStatus")
    void createResponseInfoFromRequestInfo_whenSuccess_setsSuccessfulStatus() {{
        RequestInfo requestInfo = RequestInfo.builder().apiId("api").ver("1.0").msgId("m1").ts(1L).build();
        ResponseInfo info = factory.createResponseInfoFromRequestInfo(requestInfo, true);
        assertThat(info.getStatus()).isEqualTo("successful");
        assertThat(info.getApiId()).isEqualTo("api");
    }}

    @Test
    @DisplayName("createResponseInfoFromRequestInfo_whenNullRequestInfo_failedStatus")
    void createResponseInfoFromRequestInfo_whenNullRequestInfo_failedStatus() {{
        ResponseInfo info = factory.createResponseInfoFromRequestInfo(null, false);
        assertThat(info.getStatus()).isEqualTo("failed");
    }}
}}
"""


def write_unit_test(module_dir: Path, meta: dict) -> None:
    test_class = meta["testClass"]
    path = module_dir / "src/test/java" / Path(*test_class.split("."))
    path = path.parent / (path.name + "Test.java")
    if path.exists():
        return
    kind = meta.get("kind", "response-info")
    if kind == "sanitize-html":
        content = sanitize_html_test(test_class)
    elif kind == "response-info":
        content = response_info_test(test_class)
    else:
        # fallback to response-info pattern won't compile — use sanitize if validator in name
        if "Sanitize" in test_class:
            content = sanitize_html_test(test_class)
        else:
            content = response_info_test(test_class)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)


def write_it(module_dir: Path, base_package: str, module: str) -> None:
    if base_package == "org.egov":
        test_pkg = f"org.egov.integration.{module.replace('-', '')}"
    else:
        test_pkg = base_package + ".integration"
    path = module_dir / "src/test/java" / Path(*test_pkg.split(".")) / "PostgresIntegrationIT.java"
    if path.exists():
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    content = f"""package {test_pkg};

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class PostgresIntegrationIT {{

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void postgresContainer_whenStarted_isRunning() {{
        assertThat(postgres.isRunning()).isTrue();
    }}
}}
"""
    path.write_text(content)


def patch_pom(pom: Path, jacoco_pkg: str) -> None:
    text = pom.read_text()
    if "jacoco.line.coverage" not in text:
        text = re.sub(
            r"(<java\.version>[^<]+</java\.version>)",
            r"\\1\n        <testcontainers.version>1.19.7</testcontainers.version>\n"
            r"        <jacoco.line.coverage>0.01</jacoco.line.coverage>\n"
            r"        <jacoco.branch.coverage>0.01</jacoco.branch.coverage>",
            text,
            count=1,
        )
    if "archunit-junit5" not in text and "</dependencies>" in text:
        text = text.replace("</dependencies>", TEST_DEPS + "\n    </dependencies>", 1)
    if "maven-failsafe-plugin" not in text:
        plugins = PLUGINS_BLOCK.format(jacoco_pkg=jacoco_pkg)
        if "<plugins>" in text:
            text = text.replace("<plugins>", "<plugins>" + plugins, 1)
        elif "<build>" in text and "<plugins>" not in text:
            text = text.replace("<build>", "<build>\n        <plugins>" + plugins + "\n        </plugins>", 1)
    if "<profiles>" not in text and "owasp" not in text:
        text = text.replace("</project>", OWASP_PROFILE + "\n</project>")
    pom.write_text(text)


def write_readme(module_dir: Path, module: str) -> None:
    path = module_dir / "README-TESTING.md"
    if path.exists():
        return
    path.write_text(
        f"""# {module} — testing

```bash
mvn test
mvn verify
RUN_INTEGRATION_TESTS=true mvn verify   # Testcontainers IT
mvn verify -Powasp
```

Post-deploy smoke: `core-smoke-tests` with `SERVICE={module}`.
"""
    )


def write_smoke(meta: dict) -> None:
    module = meta["module"]
    class_name = smoke_class_name(module)
    pkg = smoke_java_package(module)
    path = SMOKE_ROOT / "src/test/java" / Path(*pkg.split(".")) / f"{class_name}.java"
    if path.exists():
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        f"""package {pkg};

import org.niua.smoke.support.AbstractServiceSmokeTest;
import org.niua.smoke.support.ServiceDefinition;
import org.niua.smoke.support.ServiceRegistry;
import org.niua.smoke.support.SmokeService;

@SmokeService("{module}")
public class {class_name} extends AbstractServiceSmokeTest {{

    @Override
    protected ServiceDefinition serviceDefinition() {{
        return ServiceRegistry.require("{module}");
    }}
}}
"""
    )


def update_services_yaml(modules: list[dict]) -> None:
    yaml_path = SMOKE_ROOT / "src/test/resources/services.yaml"
    existing = yaml_path.read_text()
    lines = [existing.rstrip()]
    known = set(re.findall(r"name: ([\\w-]+)", existing))
    for meta in modules:
        name = meta["module"]
        if name in known:
            continue
        cp = meta["contextPath"].rstrip("/")
        lines.append(
            f"  - name: {name}\n    basePath: {cp}\n    healthPath: /health\n    versionPath: /info\n    dependsOn: [egov-mdms-service]"
        )
        known.add(name)
    yaml_path.write_text("\n".join(lines) + "\n")


def process_module(meta: dict, skip_pom: bool = False) -> None:
    module = meta["module"]
    module_dir = ROOT / module
    if not (module_dir / "pom.xml").exists():
        return
    jacoco_pkg = jacoco_package(meta["testClass"])
    if not skip_pom:
        patch_pom(module_dir / "pom.xml", jacoco_pkg)
    write_archunit(module_dir, meta["basePackage"])
    write_unit_test(module_dir, meta)
    write_it(module_dir, meta["basePackage"], module)
    write_readme(module_dir, module)
    write_smoke(meta)


def main() -> None:
    modules = json.loads(METADATA.read_text())
    only = sys.argv[1:] if len(sys.argv) > 1 else None
    for meta in modules:
        if only and meta["module"] not in only:
            continue
        process_module(meta)
    update_services_yaml(modules)
    print(f"Processed {len(modules)} module definitions")


if __name__ == "__main__":
    main()
