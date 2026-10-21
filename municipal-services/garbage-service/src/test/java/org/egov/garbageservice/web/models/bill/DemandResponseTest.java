package org.egov.garbageservice.web.models.bill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandResponseTest {

    @Test
    void setResponseInfo_whenRoundTrip_retainsValue() {
        DemandResponse target = new DemandResponse();
        target.setResponseInfo(null);
        assertThat(target.getResponseInfo()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new DemandResponse()).isNotNull();
    }
}
