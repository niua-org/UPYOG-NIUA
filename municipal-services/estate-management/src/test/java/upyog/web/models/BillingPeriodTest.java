package upyog.web.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BillingPeriodTest {

    @Test
    void constructor_whenGivenDates_retainsValues() {
        LocalDate from = LocalDate.of(2024, 4, 1);
        LocalDate to = LocalDate.of(2025, 3, 31);
        BillingPeriod target = new BillingPeriod(from, to);
        assertThat(target.getPeriodFrom()).isEqualTo(from);
        assertThat(target.getPeriodTo()).isEqualTo(to);
    }
}
