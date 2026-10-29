package org.egov.demand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.model.BillDetailV2;
import org.egov.demand.model.BillSearchCriteria;
import org.egov.demand.model.BillV2;
import org.egov.demand.model.BusinessServiceDetail;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandCriteria;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.model.GenerateBillCriteria;
import org.egov.demand.model.TaxHeadMaster;
import org.egov.demand.model.enums.Category;
import org.egov.demand.model.TaxHeadMasterCriteria;
import org.egov.demand.model.UpdateBillCriteria;
import org.egov.demand.model.UpdateBillRequest;
import org.egov.demand.producer.Producer;
import org.egov.demand.repository.BillRepositoryV2;
import org.egov.demand.repository.IdGenRepo;
import org.egov.demand.repository.ServiceRequestRepository;
import org.egov.demand.util.Util;
import org.egov.demand.web.contract.BillRequestV2;
import org.egov.demand.web.contract.BillResponseV2;
import org.egov.demand.web.contract.BusinessServiceDetailCriteria;
import org.egov.demand.web.contract.BusinessServiceDetailResponse;
import org.egov.demand.web.contract.RequestInfoWrapper;
import org.egov.demand.web.contract.ShortBillV2;
import org.egov.demand.web.contract.TaxHeadMasterResponse;
import org.egov.demand.web.contract.factory.ResponseFactory;
import org.egov.demand.web.validator.BillValidator;
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
class BillServicev2Test {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private ResponseFactory responseFactory;
    @Mock
    private ApplicationProperties appProps;
    @Mock
    private BillRepositoryV2 billRepository;
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
    @Mock
    private BillValidator billValidator;
    @Mock
    private Producer producer;

    @InjectMocks
    private BillServicev2 billServicev2;

    private RequestInfo requestInfo;

    @BeforeEach
    void init() {
        requestInfo = RequestInfo.builder().apiId("billing").build();
        ReflectionTestUtils.setField(billServicev2, "notifTopicName", "bill-topic");
        ReflectionTestUtils.setField(billServicev2, "billCancelTopic", "cancel-topic");
        when(responseFactory.getResponseInfo(any(), any(HttpStatus.class)))
                .thenReturn(ResponseInfo.builder().status("200 OK").build());
    }

    @Test
    void getBillResponse_and_getOwnerFieldsPlainAccessList() {
        BillV2 bill = BillV2.builder().id("b1").build();
        assertThat(billServicev2.getBillResponse(List.of(bill)).getBill()).containsExactly(bill);
        assertThat(BillServicev2.getOwnerFieldsPlainAccessList()).contains("mobileNumber", "name");
    }

    @Test
    void create_emptyBills_skipsSave() {
        BillRequestV2 request = BillRequestV2.builder().bills(Collections.emptyList()).build();
        BillResponseV2 response = billServicev2.create(request);
        verify(billRepository, never()).saveBill(any());
        assertThat(response.getBill()).isEmpty();
    }

    @Test
    void create_withBills_persists() {
        BillV2 bill = BillV2.builder().id("b1").build();
        BillRequestV2 request = BillRequestV2.builder().bills(List.of(bill)).build();
        billServicev2.create(request);
        verify(billRepository).saveBill(request);
    }

    @Test
    void sendBillToKafka_success() {
        when(appProps.getCreateBillTopic()).thenReturn("topic");
        when(appProps.getCreateBillTopicKey()).thenReturn("key");
        BillV2 bill = BillV2.builder().id("b1").build();
        BillRequestV2 request = BillRequestV2.builder().bills(List.of(bill)).build();
        assertThat(billServicev2.sendBillToKafka(request).getBill()).containsExactly(bill);
    }

    @Test
    void sendBillToKafka_failure() {
        when(appProps.getCreateBillTopic()).thenReturn("topic");
        when(appProps.getCreateBillTopicKey()).thenReturn("key");
        when(kafkaTemplate.send(anyString(), any(), any())).thenThrow(new RuntimeException("down"));
        assertThatThrownBy(() -> billServicev2.sendBillToKafka(BillRequestV2.builder().build()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void searchBill_and_searchShortBills() {
        BillV2 bill = BillV2.builder().id("b1").build();
        when(billRepository.findBill(any())).thenReturn(List.of(bill));
        ShortBillV2 shortBill = new ShortBillV2();
        when(billRepository.findShortBills(any())).thenReturn(List.of(shortBill));

        assertThat(billServicev2.searchBill(new BillSearchCriteria(), requestInfo).getBill()).containsExactly(bill);
        assertThat(billServicev2.searchShortBills(new BillSearchCriteria(), requestInfo)).containsExactly(shortBill);
    }

    @Test
    void cancelBill_multipleConsumerCodes_throws() {
        UpdateBillRequest request = UpdateBillRequest.builder()
                .UpdateBillCriteria(UpdateBillCriteria.builder()
                        .consumerCodes(Set.of("c1", "c2"))
                        .build())
                .build();
        assertThatThrownBy(() -> billServicev2.cancelBill(request)).isInstanceOf(CustomException.class);
    }

    @Test
    void cancelBill_singleConsumerCode_updatesAndNotifies() {
        UpdateBillCriteria criteria = UpdateBillCriteria.builder()
                .consumerCodes(Set.of("cc-1"))
                .tenantId("ap.kurnool")
                .businessService("TL")
                .build();
        UpdateBillRequest request = UpdateBillRequest.builder()
                .RequestInfo(requestInfo)
                .UpdateBillCriteria(criteria)
                .build();
        when(billRepository.updateBillStatus(any())).thenReturn(1);
        when(billRepository.findBill(any())).thenReturn(List.of(BillV2.builder().id("b1").build()));

        assertThat(billServicev2.cancelBill(request)).isEqualTo(1);
        verify(producer).push(anyString(), any(BillRequestV2.class));
    }

    @Test
    void generateBill_noDemands_returnsEmptyBillList() {
        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .build();
        when(demandService.getDemands(any(DemandCriteria.class), any())).thenReturn(Collections.emptyList());

        BillResponseV2 response = billServicev2.generateBill(criteria, requestInfo);
        assertThat(response.getBill()).isEmpty();
    }

    @Test
    void generateBill_withDemand_persistsBill() {
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
                                .taxAmount(BigDecimal.valueOf(50))
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

        BillResponseV2 response = billServicev2.generateBill(criteria, requestInfo);

        assertThat(response.getBill()).isNotEmpty();
        verify(billRepository).saveBill(any(BillRequestV2.class));
    }

    @Test
    void fetchBill_noExistingBill_attemptsGenerate() {
        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .build();
        when(billRepository.findBill(any())).thenReturn(Collections.emptyList());
        when(appProps.getBusinessCodeAndDemandUpdateUrlMap()).thenReturn(new HashMap<>());
        when(demandService.getDemands(any(), any())).thenReturn(Collections.emptyList());

        BillResponseV2 response = billServicev2.fetchBill(criteria, new RequestInfoWrapper(requestInfo));
        assertThat(response.getBill()).isEmpty();
        verify(billValidator).validateBillGenRequest(criteria, requestInfo);
    }

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
    void generateBill_waterService_usesAlternateDemandCriteria() {
        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("WS")
                .build();
        when(demandService.getDemands(any(DemandCriteria.class), any())).thenReturn(Collections.emptyList());

        assertThat(billServicev2.generateBill(criteria, requestInfo).getBill()).isEmpty();
        verify(demandService).getDemands(any(DemandCriteria.class), eq(requestInfo));
    }

    @Test
    void fetchBill_expiredBill_regeneratesAndMergesResponses() {
        BillDetailV2 expired = BillDetailV2.builder()
                .expiryDate(System.currentTimeMillis() - 5_000)
                .build();
        BillV2 existing = BillV2.builder()
                .consumerCode("cc-exp")
                .billDetails(List.of(expired))
                .build();
        when(billRepository.findBill(any())).thenReturn(List.of(existing));
        when(appProps.getBusinessCodeAndDemandUpdateUrlMap()).thenReturn(new HashMap<>(Map.of("TL", "http://tl/update")));
        when(demandService.getDemands(any(), any())).thenReturn(Collections.emptyList());

        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode(new HashSet<>(Set.of("cc-exp")))
                .build();

        BillResponseV2 response = billServicev2.fetchBill(criteria, new RequestInfoWrapper(requestInfo));

        verify(billRepository).updateBillStatus(any(UpdateBillCriteria.class));
        verify(restRepository).fetchResult(anyString(), any(RequestInfoWrapper.class));
        assertThat(response.getBill()).isNotNull();
    }

    @Test
    void fetchBill_validExistingBill_returnsWithoutRegeneration() {
        BillDetailV2 detail = BillDetailV2.builder()
                .expiryDate(System.currentTimeMillis() + 120_000)
                .build();
        BillV2 bill = BillV2.builder()
                .consumerCode("cc-1")
                .billDetails(List.of(detail))
                .build();
        when(billRepository.findBill(any())).thenReturn(List.of(bill));

        GenerateBillCriteria criteria = GenerateBillCriteria.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode(new HashSet<>(Set.of("cc-1")))
                .build();

        BillResponseV2 response = billServicev2.fetchBill(criteria, new RequestInfoWrapper(requestInfo));

        assertThat(response.getBill()).isNotEmpty();
    }
}
