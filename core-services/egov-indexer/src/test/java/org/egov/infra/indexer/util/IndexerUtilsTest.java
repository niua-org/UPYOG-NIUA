package org.egov.infra.indexer.util;

import com.github.zafarkhaja.semver.Version;
import org.egov.infra.indexer.web.contract.FilterMapping;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.UriMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

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

    private static void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
