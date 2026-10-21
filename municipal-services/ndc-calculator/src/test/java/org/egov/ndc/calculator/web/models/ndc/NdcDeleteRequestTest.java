package org.egov.ndc.calculator.web.models.ndc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NdcDeleteRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        NdcDeleteRequest target = new NdcDeleteRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new NdcDeleteRequest()).isNotNull();
    }
}
