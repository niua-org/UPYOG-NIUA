package org.egov.ndc.calculator.web.models.ndc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NdcApplicationRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        NdcApplicationRequest target = new NdcApplicationRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new NdcApplicationRequest()).isNotNull();
    }
}
