package org.upyog.adv.repository.querybuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.web.models.AdvertisementSearchCriteria;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdvertisementBookingQueryBuilderTest {

    @Mock
    private BookingConfiguration bookingConfiguration;

    @Test
    void getPaymentTimerInsertQuery_returnsInsertStatement() {
        var builder = new AdvertisementBookingQueryBuilder(bookingConfiguration);

        assertThat(builder.getPaymentTimerInsertQuery()).contains("INSERT");
    }

    @Test
    void getAdvertisementSearchQuery_withTenantId_addsPredicate() {
        var builder = new AdvertisementBookingQueryBuilder(bookingConfiguration);
        AdvertisementSearchCriteria criteria = AdvertisementSearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();

        String query = builder.getAdvertisementSearchQuery(criteria, params);

        assertThat(query).contains("tenant_id");
        assertThat(params).isNotEmpty();
    }
}
