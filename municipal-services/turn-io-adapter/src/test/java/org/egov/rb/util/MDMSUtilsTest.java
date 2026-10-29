package org.egov.rb.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MDMSUtilsTest {

    @InjectMocks
    private MDMSUtils mdmsUtils;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mdmsUtils, "mdmsHost", "http://mdms");
        ReflectionTestUtils.setField(mdmsUtils, "mdmsSearchEndpoint", "/mdms-v2/_search");
    }

    @Test
    void getMDMSRequest_includesTenantId() {
        RequestInfo ri = RequestInfo.builder().apiId("rb").build();
        org.egov.mdms.model.MdmsCriteriaReq req = mdmsUtils.getMDMSRequest(ri, "pb");
        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
    }
}
