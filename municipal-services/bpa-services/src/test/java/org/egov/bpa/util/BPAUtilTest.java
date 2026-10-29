package org.egov.bpa.util;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPA;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BPAUtilTest {

    @Mock
    private BPAConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private BPAUtil bpaUtil;

    @BeforeEach
    void setUp() {
        bpaUtil = new BPAUtil(config, serviceRequestRepository);
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedAndModifiedFields() {
        AuditDetails audit = bpaUtil.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
        assertThat(audit.getLastModifiedTime()).isEqualTo(audit.getCreatedTime());
    }

    @Test
    void getAuditDetails_whenUpdate_onlySetsModifiedFields() {
        AuditDetails audit = bpaUtil.getAuditDetails("user-2", false);

        assertThat(audit.getCreatedBy()).isNull();
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-2");
        assertThat(audit.getLastModifiedTime()).isNotNull();
    }

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(config.getMdmsHost()).thenReturn("http://mdms/");
        when(config.getMdmsEndPoint()).thenReturn("mdms-v2/_search");

        assertThat(bpaUtil.getMdmsSearchUrl().toString()).isEqualTo("http://mdms/mdms-v2/_search");
    }

    @Test
    void getMDMSRequest_buildsCriteriaForTenant() {
        MdmsCriteriaReq request = bpaUtil.getMDMSRequest(RequestInfo.builder().build(), "pb.amritsar");

        assertThat(request.getMdmsCriteria().getTenantId()).isEqualTo("pb.amritsar");
        assertThat(request.getMdmsCriteria().getModuleDetails()).hasSize(3);
    }

    @Test
    void getBusinessService_whenServiceTypeProvided_splitsConfiguredCodes() {
        Map<String, String> serviceMap = new LinkedHashMap<>();
        serviceMap.put("NEW_CONSTRUCTION", "BPA_NEW,BPA_ALTER");
        Map<String, Map<String, String>> appMap = new LinkedHashMap<>();
        appMap.put("BUILDING_PLAN", serviceMap);
        when(config.getAppSrvTypeBussSrvCode()).thenReturn(appMap);

        List<String> codes = bpaUtil.getBusinessService("BUILDING_PLAN", "NEW_CONSTRUCTION");

        assertThat(codes).containsExactly("BPA_NEW", "BPA_ALTER");
    }

    @Test
    void getFeeBusinessSrvCode_whenStatusMapped_returnsCode() {
        Map<String, String> statusMap = Map.of("INPROGRESS", "BPA.FEE");
        when(config.getWorkflowStatusFeeBusinessSrvMap()).thenReturn(Map.of("BPA", statusMap));

        BPA bpa = BPA.builder().businessService("BPA").status("INPROGRESS").tenantId("pb").applicationNo("BPA-1").build();

        assertThat(bpaUtil.getFeeBusinessSrvCode(bpa)).isEqualTo("BPA.FEE");
    }
}
