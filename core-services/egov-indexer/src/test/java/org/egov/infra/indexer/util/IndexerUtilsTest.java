package org.egov.infra.indexer.util;

import com.github.zafarkhaja.semver.Version;
import org.egov.infra.indexer.models.AuditDetails;
import org.egov.infra.indexer.web.contract.APIDetails;
import org.egov.infra.indexer.web.contract.FilterMapping;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.PaginationDetails;
import org.egov.infra.indexer.web.contract.ReindexRequest;
import org.egov.infra.indexer.web.contract.UriMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IndexerUtilsTest {

    private IndexerUtils indexerUtils;

    @BeforeEach
    void setUp() throws Exception {
        indexerUtils = new IndexerUtils();
        setField(indexerUtils, "defaultVersion", "1.0.0");
        Method init = IndexerUtils.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(indexerUtils);
    }

    @Test
    void buildIndexId_singleJsonPath_concatenatesFieldValues() {
        Index index = new Index();
        index.setId("$.id,$.tenantId");
        String json = "{\"id\":\"DOC-1\",\"tenantId\":\"pg.citya\"}";

        assertThat(indexerUtils.buildIndexId(index, json)).isEqualTo("DOC-1pg.citya");
    }

    @Test
    void buildIndexId_missingPath_returnsNull() {
        Index index = new Index();
        index.setId("$.missing");

        assertThat(indexerUtils.buildIndexId(index, "{\"id\":\"1\"}")).isNull();
    }

    @Test
    void buildFilter_replacesVariablesFromKafkaJson() {
        UriMapping mapping = UriMapping.builder()
                .filter("tenantId=$tenant and code=$code")
                .filterMapping(List.of(
                        FilterMapping.builder().variable("$tenant").valueJsonpath("$.tenantId").build(),
                        FilterMapping.builder().variable("$code").valueJsonpath("$.code").build()))
                .build();
        String kafkaJson = "{\"tenantId\":\"pg.citya\",\"code\":\"PT\"}";

        assertThat(indexerUtils.buildFilter(mapping, kafkaJson))
                .isEqualTo("tenantId='pg.citya' and code='PT'");
    }

    @Test
    void getSemVer_nullOrBlank_returnsDefault() {
        assertThat(indexerUtils.getSemVer(null)).isEqualTo(Version.valueOf("1.0.0"));
        assertThat(indexerUtils.getSemVer("")).isEqualTo(Version.valueOf("1.0.0"));
    }

    @Test
    void getSemVer_validVersion_parsesSemver() {
        assertThat(indexerUtils.getSemVer("2.1.0")).isEqualTo(Version.valueOf("2.1.0"));
    }

    @Test
    void getSemVer_invalidVersion_fallsBackToDefault() {
        assertThat(indexerUtils.getSemVer("not-a-version")).isEqualTo(Version.valueOf("1.0.0"));
    }

    @Test
    void getESSearchURL_andSettingsURL_useHostAndIndex() throws Exception {
        setField(indexerUtils, "esHostUrl", "http://es-host/");
        ReindexRequest request = ReindexRequest.builder().index("idx").type("typ").build();

        assertThat(indexerUtils.getESSearchURL(request)).isEqualTo("http://es-host/idx/typ/_search");
        assertThat(indexerUtils.getESSettingsURL(request)).isEqualTo("http://es-host/idx/_settings");
    }

    @Test
    void getESSearchBody_andSettingsBody_buildExpectedMaps() {
        assertThat(indexerUtils.getESSearchBody(10, 50)).isEqualTo(Map.of("from", 10, "size", 50));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Long>> settings = (Map<String, Map<String, Long>>) indexerUtils.getESSettingsBody(1000);
        assertThat(settings.get("index").get("max_result_window")).isEqualTo(51000L);
    }

    @Test
    void fetchEstimatedTime_formatsByMagnitude() {
        assertThat(indexerUtils.fetchEstimatedTime(1)).isEqualTo("less than a second");
        assertThat(indexerUtils.fetchEstimatedTime(5000)).contains("secs");
        assertThat(indexerUtils.fetchEstimatedTime(500_000)).contains("mins");
    }

    @Test
    void getAuditDetails_createAndUpdate() {
        AuditDetails created = indexerUtils.getAuditDetails("user-1", true);
        assertThat(created.getCreatedBy()).isEqualTo("user-1");
        assertThat(created.getLastModifiedBy()).isEqualTo("user-1");

        AuditDetails updated = indexerUtils.getAuditDetails("user-2", false);
        assertThat(updated.getCreatedBy()).isNull();
        assertThat(updated.getLastModifiedBy()).isEqualTo("user-2");
    }

    @Test
    void buildPagedUriForLegacyIndex_resolvesHostAndQueryParams() throws Exception {
        setField(indexerUtils, "serviceHost", "http://service-host/");
        PaginationDetails pagination = PaginationDetails.builder()
                .offsetKey("page")
                .sizeKey("limit")
                .maxPageSize(25)
                .build();
        APIDetails apiDetails = APIDetails.builder()
                .uri("/search")
                .tenantIdForOpenSearch("pg.citya")
                .customQueryParam("status=ACTIVE")
                .paginationDetails(pagination)
                .build();

        assertThat(indexerUtils.buildPagedUriForLegacyIndex(apiDetails, 5, 25))
                .isEqualTo("http://service-host//search?tenantId=pg.citya&page=5&limit=25&status=ACTIVE");
    }

    @Test
    void buildUri_resolvesPathAndQueryFromKafkaJson() throws Exception {
        UriMapping mapping = UriMapping.builder()
                .path("/resource/$tenant")
                .pathParam("$.tenantId")
                .queryParam("code=$.code")
                .build();

        assertThat(indexerUtils.buildUri(mapping, "{\"tenantId\":\"t1\",\"code\":\"PT\"}"))
                .contains("/resource/t1")
                .contains("code=PT");
    }

    @Test
    void getProcessedJsonPath_returnsParentExpression() {
        assertThat(indexerUtils.getProcessedJsonPath("$.a.b.c")).isEqualTo("$.a.b");
    }

    @Test
    void splitCamelCase_andConvertEpochToLong() {
        assertThat(indexerUtils.splitCamelCase("PropertyTax")).isEqualTo("Property Tax");
        assertThat(indexerUtils.convertEpochToLong("1.5534533434E10")).isEqualTo("15534533434");
    }

    @Test
    void encode_andGetESEncodedCredentials() throws Exception {
        setField(indexerUtils, "esUsername", "elastic");
        setField(indexerUtils, "esPassword", "secret");

        assertThat(indexerUtils.encode("hello")).contains("hello");
        assertThat(indexerUtils.getESEncodedCredentials()).startsWith("Basic ");
    }

    @Test
    void pullArrayOutOfString_stripsLeadingKey() {
        assertThat(indexerUtils.pullArrayOutOfString("payload:[1,2,3]")).isEqualTo("[1,2,3");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
