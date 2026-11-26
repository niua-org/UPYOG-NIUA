package org.egov.pt.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandGenerationRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        DemandGenerationRequest target = new DemandGenerationRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new DemandGenerationRequest()).isNotNull();
    }
}
