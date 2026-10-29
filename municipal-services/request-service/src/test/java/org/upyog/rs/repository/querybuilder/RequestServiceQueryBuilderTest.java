package org.upyog.rs.repository.querybuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.rs.config.RequestServiceConfiguration;
import org.upyog.rs.web.models.waterTanker.WaterTankerBookingSearchCriteria;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceQueryBuilderTest {

    @Mock
    private RequestServiceConfiguration requestServiceConfiguration;

    private RequestServiceQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new RequestServiceQueryBuilder(requestServiceConfiguration);
    }

    @Test
    void getWaterTankerQuery_countCall_returnsCountQueryWithTenantFilter() {
        when(requestServiceConfiguration.getIsUserProfileEnabled()).thenReturn(false);
        WaterTankerBookingSearchCriteria criteria = WaterTankerBookingSearchCriteria.builder()
                .tenantId("pb")
                .isCountCall(true)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getWaterTankerQuery(criteria, params);

        assertThat(sql).contains("count(ursbd.booking_id)");
        assertThat(sql).contains("tenant_id LIKE ?");
        assertThat(params).contains("%pb%");
    }

    @Test
    void getWaterTankerQuery_withBookingNo_addsInClause() {
        when(requestServiceConfiguration.getIsUserProfileEnabled()).thenReturn(true);
        WaterTankerBookingSearchCriteria criteria = WaterTankerBookingSearchCriteria.builder()
                .tenantId("pb")
                .bookingNo("WT-1,WT-2")
                .isCountCall(true)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getWaterTankerQuery(criteria, params);

        assertThat(sql).contains("booking_no IN (?,?)");
        assertThat(params).contains("WT-1", "WT-2");
    }
}
