package upyog.service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upyog.config.EstateConfiguration;
import upyog.repository.DemandRepository;
import upyog.util.MdmsUtil;
import upyog.web.models.billing.Demand;
import upyog.web.models.billing.DemandDetail;
import upyog.web.models.AllotmentRequest;
import upyog.web.models.Allotment;


import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class DemandService {

    @Autowired
    private EstateConfiguration config;

    @Autowired
    private CalculationService calculationService;

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private MdmsUtil mdmsUtil;


    public List<Demand> createDemand(AllotmentRequest allotmentRequest, Object mdmsData, boolean generateDemand) {
        String tenantId = allotmentRequest.getAllotments().get(0).getTenantId();
        String consumerCode = allotmentRequest.getAllotments().get(0).getAssetNo();
        Allotment allotment = allotmentRequest.getAllotments().get(0);
        User user = allotmentRequest.getRequestInfo().getUserInfo();
        log.info("user-details::"+ user);

        User owner = User.builder()
                .name(allotment.getAlloteeName())
                .emailId(allotment.getEmailId())
                .mobileNumber(allotment.getMobileNo())
                .build();

        Map<String, Object> mdmsDataMap = (Map<String, Object>) mdmsData;

        // Getting Mdms data for Estate
        List<Map<String, Object>> taxRateList = (List<Map<String, Object>>)
                ((Map<String, Object>) ((Map<String, Object>) mdmsDataMap
                        .get("MdmsRes")).get("Estate")).get("EstateCalculationType");

//        List<String> taxRateCodes = taxRateList.stream()
//                .map(tax -> (String) tax.get("feeType"))
//                .collect(Collectors.toList());

        List<DemandDetail> demandDetails = calculationService.calculateDemand(allotmentRequest, taxRateList);

        // EST service ke liye dates different logic
//        LocalDate agreementStartDate = allotment.getAgreementStartDate();
//        LocalDate agreementEndDate = allotment.getAgreementEndDate();

        //Agreement date
        LocalDate agreementStartDate = LocalDate.now();
        LocalDate agreementEndDate = LocalDate.now().plusYears(1);



        Demand demand = Demand.builder()
                .consumerCode(consumerCode)
                .demandDetails(demandDetails)
                .payer(owner)
                .tenantId(tenantId)
                .taxPeriodFrom(convertToTimestamp(agreementStartDate))
                .taxPeriodTo(convertToTimestamp(agreementEndDate))
                .consumerType(config.getModuleName())
                .businessService(config.getBusinessServiceName())
                .additionalDetails(null)
                .build();

        List<Demand> demands = new ArrayList<>();
        demands.add(demand);

        if (!generateDemand) {
            BigDecimal totalAmount = demandDetails.stream()
                    .map(DemandDetail::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            demand.setAdditionalDetails(totalAmount);
            return demands;
        }

        log.info("Sending call to billing service for generating demand for allotment id : " + consumerCode);
        return demandRepository.saveDemand(allotmentRequest.getRequestInfo(), demands);
    }

    private Long convertToTimestamp(LocalDate date) {
        return date.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

}
