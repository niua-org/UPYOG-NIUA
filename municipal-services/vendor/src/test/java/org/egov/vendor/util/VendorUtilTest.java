package org.egov.vendor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.vendor.config.VendorConfiguration;
import org.egov.vendor.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorUtilTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private VendorConfiguration vendorConfiguration;
    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private VendorUtil vendorUtil;

    @Test
    void getMdmsSearchUrl_concatenatesHostAndPath() {
        when(vendorConfiguration.getMdmsHost()).thenReturn("http://mdms");
        when(vendorConfiguration.getMdmsEndPoint()).thenReturn("/mdms-v2/_search");

        assertThat(vendorUtil.getMdmsSearchUrl().toString()).isEqualTo("http://mdms/mdms-v2/_search");
    }

    @Test
    void getMDMSRequest_buildsCriteriaWithModules() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("vendor").build();

        MdmsCriteriaReq req = vendorUtil.getMDMSRequest(requestInfo, "pb", vendorUtil.getVendorModuleRequest());

        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
        assertThat(req.getMdmsCriteria().getModuleDetails()).isNotEmpty();
    }

    @Test
    void getAuditDetails_whenCreate_populatesTimestamps() {
        var audit = vendorUtil.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
    }
}
