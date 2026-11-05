package org.egov.ndc.web.model.bill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        PaymentRequest target = new PaymentRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new PaymentRequest()).isNotNull();
    }
}
