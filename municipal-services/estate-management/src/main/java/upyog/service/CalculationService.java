package upyog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import upyog.web.models.Allotment;
import upyog.web.models.billing.DemandDetail;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class CalculationService {

    public List<DemandDetail> calculateDemand(Allotment allotment) {
        DemandDetail detail = DemandDetail.builder()
                .taxHeadMasterCode("EST_BOOKING_FEE")
                .taxAmount(allotment.getMonthlyRent())
                .collectionAmount(BigDecimal.ZERO)
                .tenantId(allotment.getTenantId())
                .build();
        log.info("Booking fee demand detail - Amount: {}", allotment.getMonthlyRent());
        return List.of(detail);
    }
}
