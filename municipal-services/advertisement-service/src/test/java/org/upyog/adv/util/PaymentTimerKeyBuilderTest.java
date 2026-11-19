package org.upyog.adv.util;

import org.junit.jupiter.api.Test;
import org.upyog.adv.web.models.AdvertisementSlotSearchCriteria;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTimerKeyBuilderTest {

    @Test
    void toRedisSlotKey_buildsDeterministicKey() {
        String key = PaymentTimerKeyBuilder.toRedisSlotKey("pb", "HOARDING", "LOC", "10x10", true,
                LocalDate.of(2024, 1, 1));

        assertThat(key).startsWith("adv:payment-timer:slot:");
        assertThat(key).contains("pb:HOARDING:LOC:10x10:true:2024-01-01");
    }

    @Test
    void toRedisTimerRowKey_whenBookingIdMissing_throws() {
        assertThatThrownBy(() -> PaymentTimerKeyBuilder.toRedisTimerRowKey("pb", "A", "L", "F", false,
                LocalDate.of(2024, 1, 1), "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildTimerDetailsList_expandsDateRange() {
        var criteria = AdvertisementSlotSearchCriteria.builder()
                .addType("HOARDING").location("LOC").faceArea("10x10").nightLight(false)
                .bookingStartDate("2024-01-01").bookingEndDate("2024-01-02")
                .build();

        var details = PaymentTimerKeyBuilder.buildTimerDetailsList(List.of(criteria), "draft-1", "user-1", "pb", 1L);

        assertThat(details).hasSize(2);
    }
}
