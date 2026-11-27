package upyog.web.models.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        DemandRequest target = new DemandRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new DemandRequest()).isNotNull();
    }
}
