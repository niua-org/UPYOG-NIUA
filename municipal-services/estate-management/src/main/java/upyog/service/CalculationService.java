package upyog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import upyog.web.models.Allotment;
import upyog.web.models.BillingCycle;
import upyog.web.models.billing.DemandDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class CalculationService {

    public List<DemandDetail> calculateDemand(BigDecimal amount, Allotment allotment) {
        DemandDetail detail = DemandDetail.builder()
                .taxHeadMasterCode("EST_BOOKING_FEE")
                .taxAmount(amount)
                .collectionAmount(BigDecimal.ZERO)
                .tenantId(allotment.getTenantId())
                .build();
        log.info("Booking fee demand detail - Amount: {}", allotment.getMonthlyRent());
        return List.of(detail);
    }

    public BigDecimal calculateAmount(
            Allotment allotment,
            LocalDate periodFrom,
            LocalDate periodTo) {

        BillingCycle cycle =
                BillingCycle.valueOf(
                        allotment.getBillingCycle().toUpperCase(Locale.ROOT));

        BigDecimal monthlyRent = allotment.getMonthlyRent();

        switch (cycle) {

            case QUARTERLY:

                LocalDate quarterEnd =
                        periodFrom.plusMonths(3).minusDays(1);

                // Full quarter
                if (quarterEnd.equals(periodTo)) {
                    return monthlyRent.multiply(BigDecimal.valueOf(3));
                }

                // Partial quarter
                long quarterChargeableDays =
                        ChronoUnit.DAYS.between(periodFrom, periodTo) + 1;

                long totalQuarterDays =
                        ChronoUnit.DAYS.between(periodFrom, quarterEnd) + 1;

                return monthlyRent.multiply(BigDecimal.valueOf(3))
                        .multiply(BigDecimal.valueOf(quarterChargeableDays))
                        .divide(
                                BigDecimal.valueOf(totalQuarterDays),
                                2,
                                RoundingMode.HALF_UP);

            case YEARLY:

                LocalDate yearEnd =
                        periodFrom.plusYears(1).minusDays(1);

                // Full year
                if (yearEnd.equals(periodTo)) {
                    return monthlyRent.multiply(BigDecimal.valueOf(12));
                }

                // Partial year
                long yearChargeableDays =
                        ChronoUnit.DAYS.between(periodFrom, periodTo) + 1;

                long totalYearDays =
                        ChronoUnit.DAYS.between(periodFrom, yearEnd) + 1;

                return monthlyRent.multiply(BigDecimal.valueOf(12))
                        .multiply(BigDecimal.valueOf(yearChargeableDays))
                        .divide(
                                BigDecimal.valueOf(totalYearDays),
                                2,
                                RoundingMode.HALF_UP);

            case MONTHLY:
            default:

                LocalDate expectedEnd =
                        periodFrom.plusMonths(1).minusDays(1);

                // Full month
                if (expectedEnd.equals(periodTo)) {
                    return monthlyRent;
                }

                // Partial month
                long chargeableDays =
                        ChronoUnit.DAYS.between(periodFrom, periodTo) + 1;

                long totalDaysInMonth =
                        YearMonth.from(periodFrom).lengthOfMonth();

                return monthlyRent
                        .divide(
                                BigDecimal.valueOf(totalDaysInMonth),
                                2,
                                RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(chargeableDays));
        }
    }
}
