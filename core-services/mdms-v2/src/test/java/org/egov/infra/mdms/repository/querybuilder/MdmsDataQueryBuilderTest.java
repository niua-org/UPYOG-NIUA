package org.egov.infra.mdms.repository.querybuilder;

import org.egov.infra.mdms.model.MdmsCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MdmsDataQueryBuilderTest {

    private MdmsDataQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new MdmsDataQueryBuilder();
    }

    @Test
    void getMdmsDataSearchQuery_withTenantAndActiveFlag() {
        MdmsCriteria criteria = MdmsCriteria.builder()
                .tenantId("pg.citya")
                .isActive(true)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getMdmsDataSearchQuery(criteria, params);

        assertThat(sql).contains("data.tenantid LIKE ?");
        assertThat(sql).contains("data.isactive = ?");
        assertThat(params).contains("pg.citya%", true);
    }
}
