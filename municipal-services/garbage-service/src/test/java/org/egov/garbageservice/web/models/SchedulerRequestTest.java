package org.egov.garbageservice.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        SchedulerRequest target = new SchedulerRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new SchedulerRequest()).isNotNull();
    }
}
