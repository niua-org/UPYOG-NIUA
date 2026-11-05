package org.egov.infra.mdms.repository.querybuilder;

import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.SchemaDefCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SchemaDefinitionQueryBuilderTest {

    @Mock
    private ApplicationConfig config;

    @InjectMocks
    private SchemaDefinitionQueryBuilder queryBuilder;

    @BeforeEach
    void defaults() {
        when(config.getDefaultOffset()).thenReturn(0);
        when(config.getDefaultLimit()).thenReturn(50);
    }

    @Test
    void getSchemaSearchQuery_withTenantAndCodes() {
        SchemaDefCriteria criteria = SchemaDefCriteria.builder()
                .tenantId("pg")
                .codes(List.of("common.StateInfo"))
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSchemaSearchQuery(criteria, params);

        assertThat(sql).contains("schema.tenantid = ?");
        assertThat(sql).contains("schema.code IN");
        assertThat(params).contains("pg", "common.StateInfo", 0, 50);
    }

    @Test
    void getSchemaSearchQuery_moduleNameTooShort_throws() {
        SchemaDefCriteria criteria = SchemaDefCriteria.builder()
                .tenantId("pg")
                .moduleName("ab")
                .build();
        List<Object> params = new ArrayList<>();

        assertThatThrownBy(() -> queryBuilder.getSchemaSearchQuery(criteria, params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum 3 characters");
    }

    @Test
    void getSchemaSearchQuery_getAllCodes_usesDistinctQuery() {
        SchemaDefCriteria criteria = SchemaDefCriteria.builder()
                .tenantId("pg")
                .isGetAllCodes(true)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSchemaSearchQuery(criteria, params);

        assertThat(sql).contains("DISTINCT ON");
        assertThat(sql).contains("tenantid = ?");
        assertThat(params).contains("pg", 0, 50);
    }
}
