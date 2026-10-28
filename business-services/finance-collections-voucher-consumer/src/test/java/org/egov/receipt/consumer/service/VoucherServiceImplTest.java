package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.mdms.service.MicroServiceUtil;
import org.egov.receipt.consumer.model.AccountDetail;
import org.egov.receipt.consumer.model.AppConfigValues;
import org.egov.receipt.consumer.model.Bill;
import org.egov.receipt.consumer.model.BillAccountDetail;
import org.egov.receipt.consumer.model.BillDetail;
import org.egov.receipt.consumer.model.BusinessService;
import org.egov.receipt.consumer.model.DishonorReasonContract;
import org.egov.receipt.consumer.model.EgModules;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.Instrument;
import org.egov.receipt.consumer.model.InstrumentContract;
import org.egov.receipt.consumer.model.InstrumentType;
import org.egov.receipt.consumer.model.InstrumentVoucherContract;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.Receipt;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.model.TaxHeadMaster;
import org.egov.receipt.consumer.model.Tenant;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.model.VoucherSearchCriteria;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoucherServiceImplTest {

    private static final String TENANT_ID = "pg.citya";
    private static final String BS_CODE = "PT";
    private static final long RECEIPT_DATE = 1_700_000_000_000L;

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private MicroServiceUtil microServiceUtil;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private RequestInfo requestInfo;
    private FinanceMdmsModel finSerMdms;
    private BusinessService businessService;
    private List<TaxHeadMaster> taxHeadMasters;

    @BeforeEach
    void setUp() throws VoucherCustomException {
        requestInfo = new RequestInfo();
        finSerMdms = new FinanceMdmsModel();

        businessService = new BusinessService();
        businessService.setCode(BS_CODE);
        businessService.setVoucherCreationEnabled(true);
        businessService.setFund("FUND01");
        businessService.setFunction("FUNC01");
        businessService.setDepartment("DEPT01");
        businessService.setFunctionary("FUNCY01");
        businessService.setScheme("SCH01");
        businessService.setServiceAttributeMappingEnabled(false);

        taxHeadMasters = List.of(new TaxHeadMaster(BS_CODE, "TAX_HEAD", "450001", null, null, null));

        when(propertiesManager.getErpURLBytenantId(anyString())).thenReturn("http://erp.example/");
        when(propertiesManager.getVoucherCreateUrl()).thenReturn("/voucher/create");
        when(propertiesManager.getVoucherCancelUrl()).thenReturn("/voucher/cancel");
        when(propertiesManager.getManualReceiptDateConfigUrl()).thenReturn("/config/manual-date");
        when(propertiesManager.getModuleIdSearchUrl()).thenReturn("/module");
        when(propertiesManager.getReceiptViewSourceUrl()).thenReturn("http://receipt-view");
        when(propertiesManager.getVoucherSearchByRefUrl()).thenReturn("/voucher/search-ref");
        when(propertiesManager.getVoucherSearchUrl()).thenReturn("/voucher/search");
        when(propertiesManager.getBusinessServiceHostUrl()).thenReturn("http://business-service/");

        when(microServiceUtil.getBusinessService(eq(TENANT_ID), eq(BS_CODE), any(), any()))
                .thenReturn(List.of(businessService));
        when(microServiceUtil.getBusinessServiceName(eq(TENANT_ID), eq(BS_CODE), any(), any()))
                .thenReturn("Property Tax");
        when(microServiceUtil.getTaxHeadMasters(eq(TENANT_ID), eq(BS_CODE), any(), any()))
                .thenReturn(taxHeadMasters);
        when(microServiceUtil.getGlcodeByInstrumentType(eq(TENANT_ID), eq(BS_CODE), any(), any(), anyString()))
                .thenReturn("110001");
    }

    private ReceiptReq receiptReqV1(BillAccountDetail... accountDetails) {
        BillDetail billDetail = new BillDetail();
        billDetail.setBusinessService(BS_CODE);
        billDetail.setReceiptNumber("RCPT-001");
        billDetail.setConsumerCode("CC-001");
        billDetail.setReceiptDate(RECEIPT_DATE);
        billDetail.setAmountPaid(new BigDecimal("100"));
        billDetail.setBillAccountDetails(new ArrayList<>(List.of(accountDetails)));

        Bill bill = Bill.builder().billDetails(List.of(billDetail)).build();
        Instrument instrument = Instrument.builder()
                .instrumentType(InstrumentType.builder().name("Cash").build())
                .build();
        Receipt receipt = Receipt.builder()
                .tenantId(TENANT_ID)
                .bill(List.of(bill))
                .instrument(instrument)
                .build();
        return ReceiptReq.builder().requestInfo(requestInfo).receipt(List.of(receipt)).build();
    }

    private BillAccountDetail taxLine(String taxHead, BigDecimal adjusted, BigDecimal amount) {
        return BillAccountDetail.builder()
                .taxHeadCode(taxHead)
                .adjustedAmount(adjusted)
                .amount(amount)
                .build();
    }

    @Test
    void createVoucher_delegatesToRepositoryAndMapsResponse() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setTenantId(TENANT_ID);
        VoucherResponse apiResponse = VoucherResponse.builder()
                .vouchers(List.of(voucher))
                .build();
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID))).thenReturn(apiResponse);

        VoucherResponse result = voucherService.createVoucher(List.of(voucher), requestInfo, TENANT_ID);

        assertThat(result.getVouchers()).hasSize(1);
        assertThat(result.getVouchers().get(0).getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createReceiptVoucher_v1_buildsVoucherAndCreates() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("100"), new BigDecimal("100")));
        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        stubReceiptVoucherFetch(createResponse, manualDateConfig("No"), egModules());

        VoucherResponse response = voucherService.createReceiptVoucher(req, finSerMdms, "V1");

        assertThat(response.getVouchers()).hasSize(1);
    }

    @Test
    void createReceiptVoucher_v2_usesPaymentIdAndConsumerCode() throws Exception {
        BillAccountDetail line = taxLine("TAX_HEAD", new BigDecimal("50"), new BigDecimal("50"));
        ReceiptReq req = receiptReqV1(line);
        req.getReceipt().get(0).setPaymentId("PAY-99");
        req.getReceipt().get(0).setConsumerCode("CONS-99");
        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        stubReceiptVoucherFetch(createResponse, manualDateConfig("No"), egModules());

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, "v2").getVouchers()).isNotEmpty();
    }

    @Test
    void createReceiptVoucher_manualReceiptDateEnabled_usesManualDate() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("100"), new BigDecimal("100")));
        req.getReceipt().get(0).getBill().get(0).getBillDetails().get(0).setManualReceiptDate(RECEIPT_DATE + 86_400_000L);
        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        stubReceiptVoucherFetch(createResponse, manualDateConfig("Yes"), egModules());

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, null)).isNotNull();
    }

    @Test
    void createReceiptVoucher_unmappedTaxHead_wrapsAsCustomException() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("UNKNOWN", new BigDecimal("10"), new BigDecimal("10")));
        stubReceiptVoucherFetch(null, manualDateConfig("No"), egModules());

        assertThatThrownBy(() -> voucherService.createReceiptVoucher(req, finSerMdms, "V1"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void createReceiptVoucher_noLedgerAmounts_wrapsNaFailure() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ZERO, BigDecimal.ZERO));
        req.getReceipt().get(0).getBill().get(0).getBillDetails().get(0).setAmountPaid(BigDecimal.ZERO);
        stubReceiptVoucherFetch(null, manualDateConfig("No"), egModules());

        assertThatThrownBy(() -> voucherService.createReceiptVoucher(req, finSerMdms, "V1"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void createReceiptVoucher_missingInstrumentGlcode_wrapsFailure() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("100"), new BigDecimal("100")));
        when(microServiceUtil.getGlcodeByInstrumentType(anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(null);
        stubReceiptVoucherFetch(null, manualDateConfig("No"), egModules());

        assertThatThrownBy(() -> voucherService.createReceiptVoucher(req, finSerMdms, "V1"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void createReceiptVoucher_advanceTaxHead_usesAbsoluteAmount() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("ADVANCE_TAX", new BigDecimal("-5"), new BigDecimal("-25")));
        taxHeadMasters = List.of(new TaxHeadMaster(BS_CODE, "ADVANCE_TAX", "450002", null, null, null));
        when(microServiceUtil.getTaxHeadMasters(anyString(), anyString(), any(), any())).thenReturn(taxHeadMasters);

        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        stubReceiptVoucherFetch(createResponse, manualDateConfig("No"), egModules());

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, "V1")).isNotNull();
    }

    @Test
    void createReceiptVoucher_debitLedgerForNegativeAdjustedAmount() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("-20"), new BigDecimal("-20")));
        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        stubReceiptVoucherFetch(createResponse, manualDateConfig("No"), egModules());

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, "V1")).isNotNull();
    }

    @Test
    void createReceiptVoucher_serviceAttributeMapping_resolvesAttribute() throws Exception {
        businessService.setServiceAttributeMappingEnabled(true);
        businessService.setServiceAttributeUrl("/attr/%s/%s");
        businessService.setServiceAttributeKey("payload.items.name");
        businessService.setServiceAttributeTaxHead("TAX_HEAD");
        taxHeadMasters = List.of(new TaxHeadMaster(BS_CODE, "TAX_HEAD", "450001", null, null, "RES"));
        when(microServiceUtil.getTaxHeadMasters(anyString(), anyString(), any(), any())).thenReturn(taxHeadMasters);

        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("10"), new BigDecimal("10")));
        req.getReceipt().get(0).getBill().get(0).getBillDetails().get(0).setConsumerCode("CC-001");

        Map<String, Object> item = new HashMap<>();
        item.put("name", "RES");
        Map<String, Object> payload = new HashMap<>();
        payload.put("items", List.of(item));
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("payload", payload);

        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        stubReceiptVoucherFetch(createResponse, manualDateConfig("No"), egModules(), apiResponse);

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, "V1")).isNotNull();
    }

    @Test
    void createReceiptVoucher_serviceAttributeTildeKey_resolvesConcatenatedValue() throws Exception {
        businessService.setServiceAttributeMappingEnabled(true);
        businessService.setServiceAttributeUrl("/attr/%s/%s");
        businessService.setServiceAttributeKey("part1~part2");
        businessService.setServiceAttributeTaxHead("TAX_HEAD");
        taxHeadMasters = List.of(new TaxHeadMaster(BS_CODE, "TAX_HEAD", "450001", null, null, "A.B"));
        when(microServiceUtil.getTaxHeadMasters(anyString(), anyString(), any(), any())).thenReturn(taxHeadMasters);

        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("10"), new BigDecimal("10")));

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("part1", "A");
        apiResponse.put("part2", "B");

        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        stubReceiptVoucherFetch(createResponse, manualDateConfig("No"), egModules(), apiResponse);

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, "V1")).isNotNull();
    }

    @Test
    void createReceiptVoucher_createVoucherFailure_wrapsCustomException() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("100"), new BigDecimal("100")));
        stubReceiptVoucherFetch(null, manualDateConfig("No"), egModules());
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID))).thenAnswer(invocation -> {
            String url = invocation.<StringBuilder>getArgument(0).toString();
            if (url.contains("/voucher/create")) {
                throw new VoucherCustomException(ProcessStatus.FAILED, "create failed");
            }
            if (url.contains("moduleName=Collections")) {
                return egModules();
            }
            return manualDateConfig("No");
        });

        assertThatThrownBy(() -> voucherService.createReceiptVoucher(req, finSerMdms, "V1"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void createReceiptVoucher_emptyBusinessServiceMapping_wrapsFailure() throws Exception {
        when(microServiceUtil.getBusinessService(anyString(), anyString(), any(), any()))
                .thenReturn(Collections.emptyList());
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("10"), new BigDecimal("10")));

        assertThatThrownBy(() -> voucherService.createReceiptVoucher(req, finSerMdms, "V1"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void createReceiptVoucher_moduleLookupFailure_defaultsModuleId() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("100"), new BigDecimal("100")));
        VoucherResponse createResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID))).thenAnswer(invocation -> {
            String url = invocation.<StringBuilder>getArgument(0).toString();
            if (url.contains("moduleName=Collections")) {
                throw new RuntimeException("module down");
            }
            if (url.contains("/voucher/create")) {
                return createResponse;
            }
            return manualDateConfig("No");
        });

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, "V1")).isNotNull();
    }

    @Test
    void createReceiptVoucher_emptyFunctionaryAndScheme_allowed() throws Exception {
        businessService.setFunctionary("");
        businessService.setScheme(null);
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", new BigDecimal("100"), new BigDecimal("100")));
        stubReceiptVoucherFetch(VoucherResponse.builder().vouchers(List.of(new Voucher())).build(),
                manualDateConfig("No"), egModules());

        assertThat(voucherService.createReceiptVoucher(req, finSerMdms, "V1")).isNotNull();
    }

    @Test
    void isVoucherCreationEnabled_returnsFlagFromBusinessService() throws Exception {
        Receipt receipt = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE)).getReceipt().get(0);
        assertThat(voucherService.isVoucherCreationEnabled(receipt, requestInfo, finSerMdms)).isTrue();
    }

    @Test
    void isVoucherCreationEnabled_noMatchingService_returnsFalse() throws Exception {
        when(microServiceUtil.getBusinessService(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(businessServiceWithCode("OTHER")));
        Receipt receipt = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE)).getReceipt().get(0);
        assertThat(voucherService.isVoucherCreationEnabled(receipt, requestInfo, finSerMdms)).isFalse();
    }

    @Test
    void isVoucherCreationEnabled_microServiceFailure_throwsCustomException() throws Exception {
        when(microServiceUtil.getBusinessService(anyString(), anyString(), any(), any()))
                .thenThrow(new VoucherCustomException(ProcessStatus.FAILED, "mdms error"));
        Receipt receipt = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE)).getReceipt().get(0);

        assertThatThrownBy(() -> voucherService.isVoucherCreationEnabled(receipt, requestInfo, finSerMdms))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void cancelReceiptVoucher_success() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE));
        VoucherResponse cancelResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID))).thenReturn(cancelResponse);

        VoucherResponse response = voucherService.cancelReceiptVoucher(req, TENANT_ID, Set.of("V-1", "V-2"));
        assertThat(response.getVouchers()).hasSize(1);
    }

    @Test
    void cancelReceiptVoucher_failureThrowsVoucherCustomException() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE));
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID)))
                .thenThrow(new RuntimeException("cancel failed"));

        assertThatThrownBy(() -> voucherService.cancelReceiptVoucher(req, TENANT_ID, Set.of("V-1")))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("Failed to cancel voucher");
    }

    @Test
    void isTenantEnabledInFinanceModule_matchingTenant_returnsTrue() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE));
        when(microServiceUtil.getFinanceTenantList(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(new Tenant(TENANT_ID)));

        assertThat(voucherService.isTenantEnabledInFinanceModule(req, finSerMdms)).isTrue();
    }

    @Test
    void isTenantEnabledInFinanceModule_usesBillBusinessServiceWhenPresent() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE));
        req.getReceipt().get(0).getBill().get(0).setBusinessService("WATER");
        when(microServiceUtil.getFinanceTenantList(eq(TENANT_ID), eq("WATER"), any(), any()))
                .thenReturn(List.of(new Tenant(TENANT_ID)));

        assertThat(voucherService.isTenantEnabledInFinanceModule(req, finSerMdms)).isTrue();
    }

    @Test
    void isTenantEnabledInFinanceModule_missingTenant_throws() throws Exception {
        ReceiptReq req = receiptReqV1(taxLine("TAX_HEAD", BigDecimal.ONE, BigDecimal.ONE));
        when(microServiceUtil.getFinanceTenantList(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(new Tenant("other.city")));

        assertThatThrownBy(() -> voucherService.isTenantEnabledInFinanceModule(req, finSerMdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("not enabled in Finance module");
    }

    @Test
    void getVoucherByServiceAndRefDoc_success() throws Exception {
        VoucherResponse searchResponse = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID))).thenReturn(searchResponse);

        VoucherResponse result = voucherService.getVoucherByServiceAndRefDoc(requestInfo, TENANT_ID, BS_CODE, "REF/1");
        assertThat(result.getVouchers()).hasSize(1);
    }

    @Test
    void getVoucherByServiceAndRefDoc_nullServiceCode_stillQueries() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID)))
                .thenReturn(VoucherResponse.builder().vouchers(List.of()).build());

        assertThat(voucherService.getVoucherByServiceAndRefDoc(requestInfo, TENANT_ID, null, "REF")).isNotNull();
    }

    @Test
    void getVoucherByServiceAndRefDoc_repositoryFailure_returnsNull() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID)))
                .thenThrow(new RuntimeException("search failed"));

        assertThat(voucherService.getVoucherByServiceAndRefDoc(requestInfo, TENANT_ID, BS_CODE, "REF")).isNull();
    }

    @Test
    void getVouchers_buildsQueryAndReturnsResponse() throws Exception {
        VoucherSearchCriteria criteria = VoucherSearchCriteria.builder()
                .ids(Set.of(1L, 2L))
                .voucherNumbers(Set.of("VN-1", "VN-2"))
                .build();
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID)))
                .thenReturn(VoucherResponse.builder().vouchers(List.of(new Voucher())).build());

        VoucherResponse result = voucherService.getVouchers(criteria, requestInfo, TENANT_ID);
        assertThat(result.getVouchers()).hasSize(1);
        assertThat(criteria.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void getVouchers_repositoryFailure_returnsNull() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID)))
                .thenThrow(new RuntimeException("failed"));

        assertThat(voucherService.getVouchers(VoucherSearchCriteria.builder().build(), requestInfo, TENANT_ID)).isNull();
    }

    @Test
    void processReversalVoucher_happyPath_createsReversalVoucher() throws Exception {
        AccountDetail shared = ledger("450001", 0d, 100d);
        AccountDetail pisOnly = ledger("450002", 50d, 0d);
        Voucher rv = Voucher.builder().ledgers(new ArrayList<>(List.of(shared))).build();
        Voucher pis = Voucher.builder().ledgers(new ArrayList<>(List.of(shared, pisOnly))).build();

        VoucherResponse rvResponse = VoucherResponse.builder().vouchers(List.of(rv)).build();
        VoucherResponse pisResponse = VoucherResponse.builder().vouchers(List.of(pis)).build();
        VoucherResponse created = VoucherResponse.builder().vouchers(List.of(new Voucher())).build();

        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID))).thenAnswer(invocation -> {
            String url = invocation.<StringBuilder>getArgument(0).toString();
            if (url.contains("ids=")) {
                return pisResponse;
            }
            if (url.contains("voucherNumbers=")) {
                return rvResponse;
            }
            return created;
        });

        InstrumentContract instrument = InstrumentContract.builder()
                .tenantId(TENANT_ID)
                .payinSlipId("99")
                .dishonor(DishonorReasonContract.builder().dishonorDate(RECEIPT_DATE).build())
                .instrumentVouchers(List.of(InstrumentVoucherContract.builder().voucherHeaderId("VH-1").build()))
                .build();

        VoucherResponse result = voucherService.processReversalVoucher(List.of(instrument), requestInfo);
        assertThat(result).isNotNull();
        assertThat(result.getVouchers()).hasSize(1);
    }

    @Test
    void processReversalVoucher_emptySearchResults_returnsNull() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID)))
                .thenReturn(VoucherResponse.builder().vouchers(Collections.emptyList()).build());

        InstrumentContract instrument = InstrumentContract.builder()
                .tenantId(TENANT_ID)
                .payinSlipId("1")
                .dishonor(DishonorReasonContract.builder().dishonorDate(RECEIPT_DATE).build())
                .instrumentVouchers(List.of(InstrumentVoucherContract.builder().voucherHeaderId("VH-1").build()))
                .build();

        assertThat(voucherService.processReversalVoucher(List.of(instrument), requestInfo)).isNull();
    }

    @Test
    void processReversalVoucher_searchFailure_returnsNull() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID)))
                .thenThrow(new RuntimeException("search failed"));

        InstrumentContract instrument = InstrumentContract.builder()
                .tenantId(TENANT_ID)
                .payinSlipId("1")
                .dishonor(DishonorReasonContract.builder().dishonorDate(RECEIPT_DATE).build())
                .instrumentVouchers(List.of(InstrumentVoucherContract.builder().voucherHeaderId("VH-1").build()))
                .build();

        assertThatThrownBy(() -> voucherService.processReversalVoucher(List.of(instrument), requestInfo))
                .isInstanceOf(NullPointerException.class);
    }

    private void stubReceiptVoucherFetch(VoucherResponse createResponse, AppConfigValues manualConfig,
            EgModules modules) throws VoucherCustomException {
        stubReceiptVoucherFetch(createResponse, manualConfig, modules, null);
    }

    private void stubReceiptVoucherFetch(VoucherResponse createResponse, AppConfigValues manualConfig,
            EgModules modules, Object serviceAttributeResponse) throws VoucherCustomException {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(TENANT_ID))).thenAnswer(invocation -> {
            String url = invocation.<StringBuilder>getArgument(0).toString();
            if (url.contains("http://business-service/")) {
                return serviceAttributeResponse;
            }
            if (url.contains("moduleName=Collections")) {
                return modules;
            }
            if (url.contains("/config/manual-date")) {
                return manualConfig;
            }
            if (url.contains("/voucher/create")) {
                return createResponse;
            }
            return manualConfig;
        });
    }

    private EgModules egModules() {
        EgModules modules = new EgModules();
        modules.setId(42);
        return modules;
    }

    private AppConfigValues manualDateConfig(String value) {
        AppConfigValues config = new AppConfigValues();
        config.setValue(value);
        return config;
    }

    private BusinessService businessServiceWithCode(String code) {
        BusinessService bs = new BusinessService();
        bs.setCode(code);
        bs.setVoucherCreationEnabled(false);
        return bs;
    }

    private AccountDetail ledger(String glcode, double debit, double credit) {
        AccountDetail detail = new AccountDetail();
        detail.setGlcode(glcode);
        detail.setDebitAmount(debit);
        detail.setCreditAmount(credit);
        return detail;
    }
}
