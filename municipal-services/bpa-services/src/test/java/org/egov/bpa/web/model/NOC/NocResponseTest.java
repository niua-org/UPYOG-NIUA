package org.egov.bpa.web.model.NOC;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NocResponseTest {

    @Test
    void setResponseInfo_whenRoundTrip_retainsValue() {
        NocResponse target = new NocResponse();
        target.setResponseInfo(null);
        assertThat(target.getResponseInfo()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new NocResponse()).isNotNull();
    }
}
