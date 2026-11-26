package org.upyog.chb.util;

import org.junit.jupiter.api.Test;
import org.upyog.chb.web.models.BookingPaymentTimerDetails;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTimerKeyBuilderTest {

    @Test
    void toRedisSlotKey_buildsExpectedPrefixAndSegments() {
        String key = PaymentTimerKeyBuilder.toRedisSlotKey("pb.amritsar", "HALL-1", "UNIT-1",
                LocalDate.of(2026, 5, 1), "09:00", "11:00");

        assertThat(key).startsWith("chb:payment-timer:slot:");
        assertThat(key).contains("pb.amritsar", "HALL-1", "UNIT-1", "2026-05-01", "09:00", "11:00");
    }

    @Test
    void toRedisValue_joinsCreatedByAndBookingId() {
        assertThat(PaymentTimerKeyBuilder.toRedisValue("user-1", "booking-1")).isEqualTo("user-1|booking-1");
    }

    @Test
    void toRedisTimerRowKey_whenRequiredFieldsMissing_throws() {
        BookingPaymentTimerDetails detail = new BookingPaymentTimerDetails();
        detail.setTenantId("pb.amritsar");

        assertThatThrownBy(() -> PaymentTimerKeyBuilder.toRedisTimerRowKey(detail, "09:00", "11:00"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
