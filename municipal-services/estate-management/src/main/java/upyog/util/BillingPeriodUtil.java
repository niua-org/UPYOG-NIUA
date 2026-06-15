package upyog.util;

import upyog.web.models.BillingCycle;
import upyog.web.models.BillingPeriod;

import java.time.LocalDate;

public class BillingPeriodUtil {

    private BillingPeriodUtil() {}

    public static BillingPeriod getBillingPeriod(
            LocalDate billingDate,
            BillingCycle cycle,
            LocalDate agreementEndDate) {

        LocalDate periodFrom = billingDate;
        LocalDate periodTo;

        switch (cycle) {

            case QUARTERLY:
                periodTo = billingDate.plusMonths(3).minusDays(1);
                break;

            case YEARLY:
                periodTo = billingDate.plusYears(1).minusDays(1);
                break;

            case MONTHLY:
            default:
                periodTo = billingDate.plusMonths(1).minusDays(1);
                break;
        }

        // If agreement ends before cycle completion,
        // generate partial cycle demand
        if (agreementEndDate != null
                && agreementEndDate.isBefore(periodTo)) {

            periodTo = agreementEndDate;
        }

        return new BillingPeriod(periodFrom, periodTo);
    }
}