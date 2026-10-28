package org.egov.noc.repository.builder;

import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.web.model.NocSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NocQueryBuilderTest {

    @Mock
    private NOCConfiguration nocConfig;

    private NocQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new NocQueryBuilder();
        ReflectionTestUtils.setField(queryBuilder, "nocConfig", nocConfig);
        ReflectionTestUtils.setField(queryBuilder, "isFuzzyEnabled", false);
    }

    @Test
    void getNocSearchQuery_withTenantAndIds_addsFilters() {
        NocSearchCriteria criteria = NocSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .ids(List.of("noc-1"))
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getNocSearchQuery(criteria, params, false);

        assertThat(sql).contains("noc.tenantid=?");
        assertThat(sql).contains("noc.id IN");
        assertThat(params).contains("pb.amritsar", "noc-1");
    }

    @Test
    void getNocSearchQuery_countMode_usesCountWrapper() {
        NocSearchCriteria criteria = NocSearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getNocSearchQuery(criteria, params, true);

        assertThat(sql).contains("COUNT(DISTINCT(noc_id))");
    }
}
