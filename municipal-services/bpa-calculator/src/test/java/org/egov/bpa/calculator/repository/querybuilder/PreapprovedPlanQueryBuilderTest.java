package org.egov.bpa.calculator.repository.querybuilder;

import org.egov.bpa.calculator.web.models.PreapprovedPlanSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreapprovedPlanQueryBuilderTest {

    private final PreapprovedPlanQueryBuilder queryBuilder = new PreapprovedPlanQueryBuilder();

    @Test
    void getPreapprovedPlanSearchQuery_withDrawingNoAndTenant_addsWhereClauses() {
        List<Object> params = new ArrayList<>();
        PreapprovedPlanSearchCriteria criteria = PreapprovedPlanSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .drawingNo("DR-9")
                .build();

        String sql = queryBuilder.getPreapprovedPlanSearchQuery(criteria, params);

        assertThat(sql).contains("eg_bpa_preapprovedplan");
        assertThat(sql).contains("ebpap.tenantid=?");
        assertThat(sql).contains("ebpap.drawing_number=?");
        assertThat(params.subList(0, 2)).containsExactly("pb.amritsar", "DR-9");
    }
}
