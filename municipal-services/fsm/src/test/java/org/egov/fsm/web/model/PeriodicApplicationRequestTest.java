package org.egov.fsm.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodicApplicationRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        PeriodicApplicationRequest target = new PeriodicApplicationRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new PeriodicApplicationRequest()).isNotNull();
    }
}
