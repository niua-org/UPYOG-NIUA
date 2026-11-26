package org.egov.demand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.model.Bill;
import org.egov.demand.model.BillAccountDetail;
import org.egov.demand.model.BillDetail;
import org.egov.demand.model.BillSearchCriteria;
import org.egov.demand.model.BusinessServiceDetail;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandCriteria;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.model.GenerateBillCriteria;
import org.egov.demand.model.TaxHeadMaster;
import org.egov.demand.model.enums.Category;
import org.egov.demand.model.TaxHeadMasterCriteria;
import org.egov.demand.repository.BillRepository;
import org.egov.demand.repository.IdGenRepo;
import org.egov.demand.repository.ServiceRequestRepository;
import org.egov.demand.util.Util;
import org.egov.demand.web.contract.BillRequest;
import org.egov.demand.web.contract.BillResponse;
import org.egov.demand.web.contract.BusinessServiceDetailCriteria;
import org.egov.demand.web.contract.BusinessServiceDetailResponse;
import org.egov.demand.web.contract.RequestInfoWrapper;
import org.egov.demand.web.contract.TaxHeadMasterResponse;
import org.egov.demand.web.contract.factory.ResponseFactory;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private ResponseFactory responseFactory;
    @Mock
    private ApplicationProperties appProps;
    @Mock
    private BillRepository billRepository;
    @Mock
    private DemandService demandService;
    @Mock
    private BusinessServDetailService businessServDetailService;
    @Mock
    private TaxHeadMasterService taxHeadService;
    @Mock
    private Util util;
    @Mock
    private ServiceRequestRepository restRepository;
    @Mock
    private IdGenRepo idGenRepo;

    @InjectMocks
    private BillService billService;

    private RequestInfo requestInfo;

    @BeforeEach
    void init() {
        requestInfo = RequestInfo.builder().apiId("billing").build();
        ReflectionTestUtils.setField(billService, "notifTopicName", "bill-topic");
        when(responseFactory.getResponseInfo(any(), any(HttpStatus.class)))
                .thenReturn(ResponseInfo.builder().status("200 OK").build());
    }

    @Test
    void getBillResponse_wrapsBills() {
        Bill bill = Bill.builder().id("b1").build();
        BillResponse response = billService.getBillResponse(List.of(bill));
        assertThat(response.getBill()).containsExactly(bill);
    }

    @Test
    void create_savesAndReturnsBills() {
        Bill bill = Bill.builder().id("b1").build();
        BillRequest request = BillRequest.builder().bills(List.of(bill)).build();
        BillResponse response = billService.create(request);
        verify(billRepository).saveBill(request);
        assertThat(response.getBill()).containsExactly(bill);
    }

    @Test
    void sendBillToKafka_success_returnsBills() {
        when(appProps.getCreateBillTopic()).thenReturn("topic");
        when(appProps.getCreateBillTopicKey()).thenReturn("key");
        Bill bill = Bill.builder().id("b1").build();
        BillRequest request = BillRequest.builder().bills(List.of(bill)).build();

        BillResponse response = billService.sendBillToKafka(request);

        verify(kafkaTemplate).send("topic", "key", request);
        assertThat(response.getBill()).containsExactly(bill);
    }

    @Test
    void sendBillToKafka_failure_throwsCustomException() {
        when(appProps.getCreateBillTopic()).thenReturn("topic");
        when(appProps.getCreateBillTopicKey()).thenReturn("key");
        when(kafkaTemplate.send(anyString(), any(), any())).thenThrow(new RuntimeException("kafka down"));

        assertThatThrownBy(() -> billService.sendBillToKafka(BillRequest.builder().bills(List.of()).build()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void searchBill_mergesTaxAmountsForSameBusinessService() {
        BillAccountDetail account = BillAccountDetail.builder().adjustedAmount(BigDecimal.ONE).build();
        BillDetail first = BillDetail.builder()
                .businessService("TL")
                .totalAmount(BigDecimal.valueOf(100))
                .billAccountDetails(List.of(account))
                .build();
        BillDetail second = BillDetail.builder()
                .businessService("TL")
                .totalAmount(BigDecimal.valueOf(50))
                .billAccountDetails(List.of(account))
                .build();
        Bill bill = Bill.builder().billDetails(List.of(first, second)).build();
        when(billRepository.findBill(any())).thenReturn(List.of(bill));

        BillResponse response = billService.searchBill(new BillSearchCriteria(), requestInfo);

        assertThat(response.getBill().get(0).getTaxAndPayments().get(0).getTaxAmount())
                .isEqualByComparingTo("150");
    }

    @Test
    void searchBill_enrichesTaxAndPayments() {
        BillAccountDetail account = BillAccountDetail.builder()
                .adjustedAmount(BigDecimal.TEN)
                .build();
        BillDetail detail = BillDetail.builder()
                .businessService("TL")
                .totalAmount(BigDecimal.valueOf(100))
                .billAccountDetails(List.of(account))
                .build();
        Bill bill = Bill.builder().billDetails(List.of(detail)).build();
        when(billRepository.findBill(any(BillSearchCriteria.class))).thenReturn(List.of(bill));

        BillResponse response = billService.searchBill(new BillSearchCriteria(), requestInfo);

        assertThat(response.getBill()).hasSize(1);
        assertThat(response.getBill().get(0).getTaxAndPayments()).hasSize(1);
    }

    @Test
    void generateBill_noDemands_throws() {
        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .build();
        when(demandService.getDemands(any(DemandCriteria.class), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> billService.generateBill(criteria, requestInfo))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void generateBill_withDemand_buildsBillAndPersists() {
        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode(Set.of("cc-1"))
                .build();
        Demand demand = Demand.builder()
                .id("dem-1")
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode("cc-1")
                .consumerType("CITIZEN")
                .minimumAmountPayable(BigDecimal.ONE)
                .taxPeriodFrom(1L)
                .taxPeriodTo(2L)
                .demandDetails(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode("TL_TAX")
                                .taxAmount(BigDecimal.valueOf(100))
                                .collectionAmount(BigDecimal.ZERO)
                                .build()))
                .build();
        when(demandService.getDemands(any(DemandCriteria.class), any())).thenReturn(List.of(demand));

        TaxHeadMaster taxHead = taxHead("TL_TAX", "TL");
        when(taxHeadService.getTaxHeads(any(TaxHeadMasterCriteria.class), any()))
                .thenReturn(TaxHeadMasterResponse.builder().taxHeadMasters(List.of(taxHead)).build());

        BusinessServiceDetail business = BusinessServiceDetail.builder()
                .code("TL")
                .collectionModesNotAllowed(List.of())
                .partPaymentAllowed(true)
                .isAdvanceAllowed(false)
                .build();
        when(businessServDetailService.searchBusinessServiceDetails(any(BusinessServiceDetailCriteria.class), any()))
                .thenReturn(BusinessServiceDetailResponse.builder().businessServiceDetails(List.of(business)).build());

        when(appProps.getBillNumberFormat()).thenReturn("TL/{module}/{tenant}");
        when(appProps.getModuleReplaceStirng()).thenReturn("{module}");
        when(appProps.getTenantIdReplaceString()).thenReturn("{tenant}");
        when(appProps.getIsTenantLevelBillNumberingEnabled()).thenReturn(false);
        when(idGenRepo.getId(any(), anyString(), anyString(), anyString(), anyInt())).thenReturn(List.of("BN-1"));
        when(util.getAuditDetail(any())).thenReturn(null);

        BillResponse response = billService.generateBill(criteria, requestInfo);

        assertThat(response.getBill()).isNotEmpty();
        verify(billRepository).saveBill(any(BillRequest.class));
        verify(kafkaTemplate).send(anyString(), any(), any(BillRequest.class));
    }

    @Test
    void fetchBill_existingNonExpiredBill_returnsSearchResult() {
        BillDetail detail = BillDetail.builder()
                .businessService("TL")
                .consumerCode("cc-1")
                .expiryDate(System.currentTimeMillis() + 60_000)
                .build();
        Bill bill = Bill.builder().billDetails(List.of(detail)).build();
        when(billRepository.findBill(any())).thenReturn(List.of(bill));

        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .build();
        RequestInfoWrapper wrapper = new RequestInfoWrapper(requestInfo);

        BillResponse response = billService.fetchBill(criteria, wrapper);

        assertThat(response.getBill()).containsExactly(bill);
        verify(demandService, org.mockito.Mockito.never()).getDemands(any(), any());
    }

    @Test
    void fetchBill_noExistingBill_generatesNewBill() {
        when(billRepository.findBill(any())).thenReturn(Collections.emptyList());
        when(demandService.getDemands(any(), any())).thenReturn(Collections.emptyList());

        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .build();
        RequestInfoWrapper wrapper = new RequestInfoWrapper(requestInfo);

        assertThatThrownBy(() -> billService.fetchBill(criteria, wrapper))
                .isInstanceOf(CustomException.class);
    }

    @Test
    private static TaxHeadMaster taxHead(String code, String service) {
        TaxHeadMaster taxHead = new TaxHeadMaster();
        taxHead.setCode(code);
        taxHead.setService(service);
        taxHead.setTenantId("ap.kurnool");
        taxHead.setCategory(Category.TAX);
        taxHead.setName("Tax");
        taxHead.setValidFrom(1L);
        taxHead.setValidTill(2L);
        return taxHead;
    }

    @Test
    void fetchBill_expiredBillDetail_triggersDemandUpdateAndRegenerate() {
        BillDetail expiredDetail = BillDetail.builder()
                .businessService("TL")
                .consumerCode("cc-1")
                .expiryDate(System.currentTimeMillis() - 10_000)
                .build();
        Bill bill = Bill.builder().billDetails(List.of(expiredDetail)).build();
        when(billRepository.findBill(any())).thenReturn(List.of(bill));

        Map<String, String> urlMap = new HashMap<>();
        urlMap.put("TL", "http://tl-service/update");
        when(appProps.getBusinessCodeAndDemandUpdateUrlMap()).thenReturn(urlMap);

        Demand demand = Demand.builder()
                .id("dem-1")
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode("cc-1")
                .consumerType("CITIZEN")
                .minimumAmountPayable(BigDecimal.ONE)
                .taxPeriodFrom(1L)
                .taxPeriodTo(2L)
                .demandDetails(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode("TL_TAX")
                                .taxAmount(BigDecimal.TEN)
                                .collectionAmount(BigDecimal.ZERO)
                                .build()))
                .build();
        when(demandService.getDemands(any(DemandCriteria.class), any())).thenReturn(List.of(demand));

        TaxHeadMaster taxHead = taxHead("TL_TAX", "TL");
        when(taxHeadService.getTaxHeads(any(TaxHeadMasterCriteria.class), any()))
                .thenReturn(TaxHeadMasterResponse.builder().taxHeadMasters(List.of(taxHead)).build());
        BusinessServiceDetail business = BusinessServiceDetail.builder()
                .code("TL")
                .collectionModesNotAllowed(List.of())
                .partPaymentAllowed(true)
                .isAdvanceAllowed(false)
                .build();
        when(businessServDetailService.searchBusinessServiceDetails(any(BusinessServiceDetailCriteria.class), any()))
                .thenReturn(BusinessServiceDetailResponse.builder().businessServiceDetails(List.of(business)).build());
        when(appProps.getBillNumberFormat()).thenReturn("TL/{module}/{tenant}");
        when(appProps.getModuleReplaceStirng()).thenReturn("{module}");
        when(appProps.getTenantIdReplaceString()).thenReturn("{tenant}");
        when(appProps.getIsTenantLevelBillNumberingEnabled()).thenReturn(false);
        when(idGenRepo.getId(any(), anyString(), anyString(), anyString(), anyInt())).thenReturn(List.of("BN-2"));
        when(util.getAuditDetail(any())).thenReturn(null);

        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .build();

        BillResponse response = billService.fetchBill(criteria, new RequestInfoWrapper(requestInfo));

        verify(restRepository).fetchResult(anyString(), any(RequestInfoWrapper.class));
        assertThat(response.getBill()).isNotEmpty();
    }

    @Test
    void apportion_delegatesToRepository() {
        Bill bill = Bill.builder().id("b1").build();
        BillRequest request = BillRequest.builder().requestInfo(requestInfo).bills(List.of(bill)).build();
        when(billRepository.apportion(request)).thenReturn(List.of(bill));

        BillResponse response = billService.apportion(request);

        assertThat(response.getBill()).containsExactly(bill);
    }
}
