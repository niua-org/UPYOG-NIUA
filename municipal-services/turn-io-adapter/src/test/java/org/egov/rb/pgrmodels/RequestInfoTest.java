package org.egov.rb.pgrmodels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestInfoTest {

    @Test
    void setApiId_whenRoundTrip_retainsValue() {
        RequestInfo target = new RequestInfo();
        target.setApiId("test");
        assertThat(target.getApiId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new RequestInfo()).isNotNull();
    }
}
