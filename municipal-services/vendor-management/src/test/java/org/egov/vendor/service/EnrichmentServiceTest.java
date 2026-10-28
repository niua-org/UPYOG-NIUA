package org.egov.vendor.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.vendor.config.VendorConfiguration;
import org.egov.vendor.repository.IdGenRepository;
import org.egov.vendor.utils.VendorUtil;
import org.egov.vendor.web.models.AuditDetails;
import org.egov.vendor.web.models.VendorAdditionalDetails;
import org.egov.vendor.web.models.VendorAdditionalDetailsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {

    @Mock
    private VendorConfiguration config;
    @Mock
    private VendorUtil vendorUtil;
    @Mock
    private IdGenRepository idGenRepository;

    @InjectMocks
    private EnrichmentService enrichmentService;

    @Test
    void enrichAssetOtherOperationsUpdateRequest_setsAuditDetails() {
        when(vendorUtil.getAuditDetails(eq("uuid-1"), eq(true)))
                .thenReturn(AuditDetails.builder().createdBy("uuid-1").build());
        VendorAdditionalDetailsRequest request = VendorAdditionalDetailsRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("uuid-1").build()).build())
                .vendorAdditionalDetails(new VendorAdditionalDetails())
                .build();

        enrichmentService.enrichAssetOtherOperationsUpdateRequest(request);

        assertThat(request.getVendorAdditionalDetails().getAuditDetails()).isNotNull();
    }
}
