package upyog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import digit.models.coremodels.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upyog.config.EstateConfiguration;
import upyog.repository.EstateRepository;
import upyog.web.models.RentPaymentDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentUpdateService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EstateConfiguration config;

    @Autowired
    private EstateRepository estateRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public void process(HashMap<String, Object> record, String topic) throws JsonProcessingException {
        try {
            PaymentRequest paymentRequest = mapper.convertValue(record, PaymentRequest.class);
            String businessService = paymentRequest.getPayment().getPaymentDetails().get(0).getBusinessService();

            if (!config.getBusinessServiceName().equals(businessService)) {
                log.debug("Ignoring payment for businessService: {}", businessService);
                return;
            }

            String consumerCode = paymentRequest.getPayment().getPaymentDetails().get(0).getBill().getConsumerCode();
            BigDecimal paidAmount = paymentRequest.getPayment().getPaymentDetails().get(0).getTotalAmountPaid();
            String tenantId = paymentRequest.getPayment().getTenantId();
            String userUuid = paymentRequest.getRequestInfo().getUserInfo() != null
                    ? paymentRequest.getRequestInfo().getUserInfo().getUuid() : "system";
            long now = System.currentTimeMillis();
            LocalDate today = LocalDate.now();

            log.info("Processing payment for consumerCode: {}, amount: {}", consumerCode, paidAmount);

            RentPaymentDetails rentPaymentDetails = RentPaymentDetails.builder()
                    .id(UUID.randomUUID().toString())
                    .allotmentId(consumerCode)
                    .rent(paidAmount)
                    .paymentType("MONTHLY_RENT")
                    .penaltyAmount(BigDecimal.ZERO)
                    .previousMonth(today.minusMonths(1).withDayOfMonth(1))
                    .paymentDate(today)
                    .lastDateOfPayment(today)
                    .duePaymentDate(today.plusMonths(1).withDayOfMonth(1))
                    .paymentStatus("PAID")
                    .duePayment(BigDecimal.ZERO)
                    .validityDays(30)
                    .createdBy(userUuid)
                    .lastModifiedBy(userUuid)
                    .createdTime(now)
                    .lastModifiedTime(now)
                    .build();

            estateRepository.save(config.getMonthlyRentPaymentSaveTopic(), Map.of("rentPaymentDetails", rentPaymentDetails));
            log.info("RentPaymentDetails saved to ug_em_monthly_rent_payment for consumerCode: {}", consumerCode);

        } catch (Exception e) {
            log.error("Error processing payment update for estate: {}", e.getMessage(), e);
        }
    }
}
