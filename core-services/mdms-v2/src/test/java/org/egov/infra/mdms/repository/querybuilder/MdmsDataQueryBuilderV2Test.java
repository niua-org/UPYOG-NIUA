package org.egov.infra.mdms.repository.querybuilder;

import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.MdmsCriteriaV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MdmsDataQueryBuilderV2Test {

    @Mock
    private ApplicationConfig config;

    @InjectMocks
    private MdmsDataQueryBuilderV2 queryBuilder;

    @BeforeEach
    void defaults() {
        when(config.getDefaultOffset()).thenReturn(0);
        when(config.getDefaultLimit()).thenReturn(50);
    }

    @Test
    void getMdmsDataSearchQuery_tenantAndSchemaCode() {
        MdmsCriteriaV2 criteria = MdmsCriteriaV2.builder()
                .tenantId("pg.citya")
                .schemaCode("common.StateInfo")
                .isActive(true)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getMdmsDataSearchQuery(criteria, params);

        assertThat(sql).contains("data.tenantid = ?");
        assertThat(sql).contains("data.schemacode = ?");
        assertThat(sql).contains("data.isactive = ?");
        assertThat(sql).contains("OFFSET ?");
        assertThat(sql).contains("LIMIT ?");
        assertThat(params).contains("pg.citya", "common.StateInfo", true, 0, 50);
    }

    @Test
    void getMdmsDataSearchQuery_idsAndFilterMap() {
        Map<String, String> filters = new HashMap<>();
        filters.put("code", "ABC");
        MdmsCriteriaV2 criteria = MdmsCriteriaV2.builder()
                .ids(java.util.Set.of("id-1", "id-2"))
                .filterMap(filters)
                .limit(10)
                .offset(5)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getMdmsDataSearchQuery(criteria, params);

        assertThat(sql).contains("data.id IN");
        assertThat(sql).contains("data.data @> CAST( ? AS jsonb )");
        assertThat(params).contains(5, 10);
    }
}
