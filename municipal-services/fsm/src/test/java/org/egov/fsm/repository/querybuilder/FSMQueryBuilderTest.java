package org.egov.fsm.repository.querybuilder;

import org.egov.fsm.config.FSMConfiguration;
import org.egov.fsm.web.model.FSMSearchCriteria;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FSMQueryBuilderTest {

    @Mock
    private FSMConfiguration config;

    @InjectMocks
    private FSMQueryBuilder queryBuilder;

    @BeforeEach
    void stubConfig() {
        when(config.getDefaultLimit()).thenReturn(10);
        when(config.getDefaultOffset()).thenReturn(0);
    }

    @Test
    void getFSMSearchQuery_withCityTenant_addsExactTenantPredicate() {
        FSMSearchCriteria criteria = FSMSearchCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        String query = queryBuilder.getFSMSearchQuery(criteria, null, params);

        assertThat(query).contains("fsm.tenantid=?");
        assertThat(params).contains("pb.amritsar");
    }

    @Test
    void getFSMSearchQuery_withStateTenant_usesLikePredicate() {
        FSMSearchCriteria criteria = FSMSearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();

        String query = queryBuilder.getFSMSearchQuery(criteria, null, params);

        assertThat(query).contains("fsm.tenantid like ?");
        assertThat(params).contains("%pb%");
    }

    @Test
    void staticQueries_areDefined() {
        assertThat(FSMQueryBuilder.GET_UNIQUE_TENANTS).contains("distinct(tenantid)");
        assertThat(FSMQueryBuilder.GET_VEHICLE_TRIPS_LIST).contains("eg_vehicle_trip_detail");
    }

    @Test
    void getFSMSearchQuery_withApplicationNos_addsLikeClauses() {
        FSMSearchCriteria criteria = FSMSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .applicationNos(List.of("FSM-12"))
                .build();
        List<Object> params = new ArrayList<>();

        String query = queryBuilder.getFSMSearchQuery(criteria, null, params);

        assertThat(query).contains("fsm.applicationNo LIKE ?");
        assertThat(params).contains("%FSM-12%");
    }
}
