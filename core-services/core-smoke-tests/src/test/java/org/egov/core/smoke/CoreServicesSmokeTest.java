package org.egov.core.smoke;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.egov.core.smoke.support.AuthClient;
import org.egov.core.smoke.support.Polling;
import org.egov.core.smoke.support.RequestSpecFactory;
import org.egov.core.smoke.support.SmokeConfig;
import org.egov.core.smoke.support.TenantContext;
import org.egov.core.smoke.support.TestDataFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * One parameterized smoke class for every catalog entry. Empty paths are skipped.
 * {@code -Dservice=} / {@code SERVICE} selects a name; {@code all} and {@code ALL} run every entry.
 */
@Tag("smoke")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoreServicesSmokeTest {

    private static SmokeConfig config;
    private static RequestSpecification spec;

    @BeforeAll
    static void loadConfig() {
        config = SmokeConfig.load();
        spec = RequestSpecFactory.spec(config);
    }

    static Stream<ServiceDef> services() {
        InputStream in = CoreServicesSmokeTest.class.getResourceAsStream("/services.yaml");
        if (in == null) {
            throw new IllegalStateException("services.yaml is not on the classpath");
        }
        Object loaded = new Yaml().load(in);
        if (!(loaded instanceof Map<?, ?> root)) {
            throw new IllegalStateException("services.yaml must be a map");
        }
        Object raw = root.get("services");
        if (!(raw instanceof List<?> list)) {
            throw new IllegalStateException("services.yaml has no services list");
        }
        List<ServiceDef> defs = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                defs.add(ServiceDef.from(map));
            }
        }
        return defs.stream();
    }

    @ParameterizedTest(name = "{0} health")
    @MethodSource("services")
    @Tag("smoke")
    void healthIsUp(ServiceDef service) {
        selected(service);
        Assumptions.assumeTrue(service.has(service.healthPath()),
                "health path not confirmed in application.properties or application.yml");
        String url = endpoint(service.basePath(), service.healthPath());
        Polling.until(() -> healthy(given().spec(spec).get(url)));
    }

    @ParameterizedTest(name = "{0} version")
    @MethodSource("services")
    @Tag("smoke")
    void versionMatchesWhenExpected(ServiceDef service) {
        selected(service);
        Assumptions.assumeTrue(config.expectedVersion().isPresent() && service.has(service.versionPath()),
                "version check skipped: EXPECTED_VERSION or versionPath is not set");
        String expected = config.expectedVersion().orElseThrow();
        Response response = given().spec(spec).get(endpoint(service.basePath(), service.versionPath()));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).contains(expected);
    }

    @ParameterizedTest(name = "{0} read")
    @MethodSource("services")
    @Tag("smoke")
    void coreRead(ServiceDef service) {
        selected(service);
        Assumptions.assumeTrue(service.has(service.readPath()),
                "read path not confirmed; skipped");
        Response response = given().spec(spec).get(endpoint(service.basePath(), service.readPath()));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @ParameterizedTest(name = "{0} write")
    @MethodSource("services")
    @Tag("smoke")
    void writeFlow(ServiceDef service) {
        selected(service);
        Assumptions.assumeTrue(service.has(service.writePath()),
                "write path not confirmed; create API was not taken from application config");
        TenantContext tenants = TenantContext.from(config);
        TestDataFactory data = TestDataFactory.from(config);
        assertThat(data.tenantId()).isEqualTo(tenants.smokeTenant());
        assertThat(data.marker()).startsWith("SMOKE_");
        String token = AuthClient.from(config).accessToken();
        Response response = given().spec(spec)
                .header("Auth-Token", token)
                .post(endpoint(service.basePath(), service.writePath()));
        assertThat(response.statusCode()).isBetween(200, 299);
    }

    @ParameterizedTest(name = "{0} auth")
    @MethodSource("services")
    @Tag("smoke")
    void unauthenticatedIsRejected(ServiceDef service) {
        selected(service);
        Assumptions.assumeTrue(service.has(service.authPath()),
                "auth path not confirmed; service enforcement was not taken from application config");
        Response response = given().spec(spec).get(endpoint(service.basePath(), service.authPath()));
        assertThat(response.statusCode()).isIn(401, 403);
    }

    @ParameterizedTest(name = "{0} tenant")
    @MethodSource("services")
    @Tag("smoke")
    void tenantIsolation(ServiceDef service) {
        selected(service);
        Assumptions.assumeTrue(service.has(service.tenantPath()),
                "tenant-scoped path not confirmed; isolation skipped");
        TenantContext tenants = TenantContext.from(config);
        assertThat(tenants.otherTenant()).isNotEqualTo(tenants.smokeTenant());
        String path = endpoint(service.basePath(), service.tenantPath());
        Response smoke = given().spec(spec).queryParam("tenantId", tenants.smokeTenant()).get(path);
        Response other = given().spec(spec).queryParam("tenantId", tenants.otherTenant()).get(path);
        assertThat(smoke.statusCode()).isEqualTo(200);
        assertThat(other.statusCode()).isEqualTo(200);
        assertThat(other.asString()).doesNotContain(tenants.smokeTenant());
    }

    private static void selected(ServiceDef service) {
        Assumptions.assumeTrue(config.matchesService(service.name()),
                "filtered by SERVICE/-Dservice=" + config.service());
    }

    private static boolean healthy(Response response) {
        if (response.statusCode() != 200) {
            return false;
        }
        String body = response.getBody() == null ? "" : response.asString();
        return body.isBlank() || body.contains("UP");
    }

    static String endpoint(String basePath, String path) {
        String suffix = path.startsWith("/") ? path : "/" + path;
        if (basePath == null || basePath.isBlank()) {
            return suffix;
        }
        String prefix = basePath.startsWith("/") ? basePath : "/" + basePath;
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (suffix.equals(prefix) || suffix.startsWith(prefix + "/")) {
            return suffix;
        }
        return prefix + suffix;
    }

    record ServiceDef(
            String name,
            String basePath,
            String healthPath,
            String versionPath,
            String readPath,
            String writePath,
            String authPath,
            String tenantPath) {

        static ServiceDef from(Map<?, ?> map) {
            return new ServiceDef(
                    text(map.get("name")),
                    text(map.get("basePath")),
                    text(map.get("healthPath")),
                    text(map.get("versionPath")),
                    text(map.get("readPath")),
                    text(map.get("writePath")),
                    text(map.get("authPath")),
                    text(map.get("tenantPath")));
        }

        boolean has(String value) {
            return value != null && !value.isBlank();
        }

        @Override
        public String toString() {
            return name;
        }

        private static String text(Object value) {
            return value == null ? "" : value.toString().trim();
        }
    }
}
