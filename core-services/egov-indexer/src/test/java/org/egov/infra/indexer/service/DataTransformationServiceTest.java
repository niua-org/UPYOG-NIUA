package org.egov.infra.indexer.service;

import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.web.contract.CustomJsonMapping;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataTransformationServiceTest {

    @Mock
    private IndexerUtils indexerUtils;

    private DataTransformationService service;

    @BeforeEach
    void setUp() {
        service = new DataTransformationService();
        setField(service, "indexerUtils", indexerUtils);
    }

    @Test
    void appendIdToJson_emptyId_returnsNull() {
        Index index = new Index();
        when(indexerUtils.buildIndexId(any(), anyString())).thenReturn("");

        assertThat(service.appendIdToJson(index, new StringBuilder(), "{}", null)).isNull();
    }

    @Test
    void appendIdToJson_validId_appendsBulkHeaderAndPayload() {
        Index index = new Index();
        when(indexerUtils.buildIndexId(any(), anyString())).thenReturn("doc-1");
        StringBuilder builder = new StringBuilder();

        StringBuilder result = service.appendIdToJson(index, builder, "{\"id\":\"1\"}", null);

        assertThat(result).isSameAs(builder);
        assertThat(result.toString()).contains("doc-1");
        assertThat(result.toString()).contains("{\"id\":\"1\"}");
    }

    @Test
    void buildCustomJsonForIndex_missingIndexMapping_throwsMissingCustomMapping() {
        CustomJsonMapping mapping = new CustomJsonMapping();

        assertThatThrownBy(() -> service.buildCustomJsonForIndex(mapping, "{}"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EGOV_INDEXER_MISSING_CUSTOM_MAPPING");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
