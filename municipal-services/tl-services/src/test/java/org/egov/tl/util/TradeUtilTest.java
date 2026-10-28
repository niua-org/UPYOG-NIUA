package org.egov.tl.util;

import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.web.models.AuditDetails;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseDetail;
import org.egov.tl.web.models.TradeUnit;
import org.egov.tl.web.models.workflow.BusinessService;
import org.egov.tl.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.egov.tl.util.TLConstants.STATUS_INITIATED;
import static org.egov.tl.util.TLConstants.businessService_BPA;
import static org.egov.tl.util.TLConstants.businessService_TL;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeUtilTest {

    @Mock
    private TLConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private TradeUtil tradeUtil;

    @BeforeEach
    void stubCalculatorEndpoints() {
        when(config.getCalculatorHost()).thenReturn("http://calc");
        when(config.getCalculateEndpointTL()).thenReturn("/tl/_calculate");
        when(config.getCalculateEndpointBPA()).thenReturn("/bpa/_calculate");
        when(config.getEstimateEndpointTL()).thenReturn("/tl/_estimate");
        when(config.getPropertyHost()).thenReturn("http://pt");
        when(config.getPropertyContextPath()).thenReturn("/pt");
        when(config.getPropertySearchEndpoint()).thenReturn("/property/_search");
        when(config.getMdmsHost()).thenReturn("http://mdms");
        when(config.getMdmsV1EndPoint()).thenReturn("/mdms-v1/_search");
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedAndModified() {
        AuditDetails details = tradeUtil.getAuditDetails("user-1", true);
        assertThat(details.getCreatedBy()).isEqualTo("user-1");
        assertThat(details.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(details.getCreatedTime()).isNotNull();
        assertThat(details.getLastModifiedTime()).isEqualTo(details.getCreatedTime());
    }

    @Test
    void getAuditDetails_whenUpdate_onlyLastModified() {
        AuditDetails details = tradeUtil.getAuditDetails("user-2", false);
        assertThat(details.getCreatedBy()).isNull();
        assertThat(details.getLastModifiedBy()).isEqualTo("user-2");
        assertThat(details.getLastModifiedTime()).isNotNull();
    }

    @Test
    void getCalculationURI_whenBusinessServiceNull_defaultsToTlEndpoint() {
        String uri = tradeUtil.getCalculationURI(null).toString();
        assertThat(uri).isEqualTo("http://calc/tl/_calculate");
    }

    @Test
    void getCalculationURI_whenBpa_usesBpaEndpoint() {
        String uri = tradeUtil.getCalculationURI(businessService_BPA).toString();
        assertThat(uri).isEqualTo("http://calc/bpa/_calculate");
    }

    @Test
    void getPropertySearchURL_containsTenantAndPropertyPlaceholders() {
        assertThat(tradeUtil.getPropertySearchURL())
                .contains("tenantId=")
                .contains("{1}")
                .contains("propertyIds=")
                .contains("{2}");
    }

    @Test
    void getTaxPeriods_whenFinancialYearFound_returnsStartAndEnd() {
        Map<String, Object> mdmsData = Map.of(
                "MdmsRes",
                Map.of(
                        "egf-master",
                        Map.of(
                                "FinancialYear",
                                List.of(
                                        Map.of(
                                                "code",
                                                "2024-25",
                                                TLConstants.MDMS_STARTDATE,
                                                1000L,
                                                TLConstants.MDMS_ENDDATE,
                                                2000L)))));
        TradeLicense license =
                TradeLicense.builder().financialYear("2024-25").build();

        Map<String, Long> periods = tradeUtil.getTaxPeriods(license, mdmsData);

        assertThat(periods).containsEntry(TLConstants.MDMS_STARTDATE, 1000L);
        assertThat(periods).containsEntry(TLConstants.MDMS_ENDDATE, 2000L);
    }

    @Test
    void getTaxPeriods_whenFinancialYearMissing_throwsCustomException() {
        TradeLicense license = TradeLicense.builder().financialYear("2099-00").build();
        assertThatThrownBy(() -> tradeUtil.getTaxPeriods(license, Map.of("MdmsRes", Map.of())))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("No data found for the financialYear");
    }

    @Test
    void getIdToIsStateUpdatableMap_whenBpaInitiated_marksUpdatable() {
        TradeLicense license = TradeLicense.builder()
                .id("lic-1")
                .businessService(businessService_BPA)
                .status(STATUS_INITIATED)
                .build();

        Map<String, Boolean> map =
                tradeUtil.getIdToIsStateUpdatableMap(BusinessService.builder().build(), List.of(license));

        assertThat(map).containsEntry("lic-1", true);
    }

    @Test
    void getIdToIsStateUpdatableMap_whenTl_delegatesToWorkflowService() {
        when(workflowService.isStateUpdatable("APPLIED", null)).thenReturn(false);
        TradeLicense license = TradeLicense.builder()
                .id("lic-2")
                .businessService(businessService_TL)
                .status("APPLIED")
                .build();

        Map<String, Boolean> map =
                tradeUtil.getIdToIsStateUpdatableMap(BusinessService.builder().build(), List.of(license));

        assertThat(map).containsEntry("lic-2", false);
    }

    @Test
    void getTradeModuleRequest_includesTlAndCommonMasters() {
        assertThat(tradeUtil.getTradeModuleRequest()).hasSize(2);
    }
}
