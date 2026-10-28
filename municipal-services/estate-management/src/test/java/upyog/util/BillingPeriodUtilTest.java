package upyog.util;

import org.junit.jupiter.api.Test;
import upyog.web.models.BillingCycle;
import upyog.web.models.BillingPeriod;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BillingPeriodUtilTest {

    @Test
    void getBillingPeriod_monthly_returnsOneMonthWindow() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        BillingPeriod period = BillingPeriodUtil.getBillingPeriod(start, BillingCycle.MONTHLY, null);
        assertThat(period.getPeriodFrom()).isEqualTo(start);
        assertThat(period.getPeriodTo()).isEqualTo(LocalDate.of(2024, 1, 31));
    }
}
