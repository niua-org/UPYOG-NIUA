package upyog.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllotmentRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        AllotmentRequest target = new AllotmentRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new AllotmentRequest()).isNotNull();
    }
}
