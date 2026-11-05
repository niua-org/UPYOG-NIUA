package org.upyog.chb.repository.querybuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.web.models.VenueBookingSearchCriteria;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommunityHallBookingQueryBuilderTest {

    @Mock
    private CommunityHallBookingConfiguration bookingConfiguration;

    @InjectMocks
    private CommunityHallBookingQueryBuilder queryBuilder;

    @Test
    void getSlotDetailsQuery_includesPlaceholdersForEachBookingId() {
        String query = queryBuilder.getSlotDetailsQuery(List.of("b1", "b2"));

        assertThat(query).contains("eg_chb_slot_detail");
        assertThat(query).contains("?, ?)");
    }

    @Test
    void getCommunityHallBookingSearchQuery_withTenantFilter_addsTenantPredicate() {
        VenueBookingSearchCriteria criteria = VenueBookingSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .build();
        criteria.setCountCall(true);
        List<Object> params = new ArrayList<>();

        String query = queryBuilder.getCommunityHallBookingSearchQuery(criteria, params);

        assertThat(query).contains("tenant_id=?");
        assertThat(params).containsExactly("pb.amritsar");
    }
}
