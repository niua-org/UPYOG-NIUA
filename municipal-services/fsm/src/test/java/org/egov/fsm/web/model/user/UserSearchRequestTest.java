package org.egov.fsm.web.model.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSearchRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        UserSearchRequest target = new UserSearchRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new UserSearchRequest()).isNotNull();
    }
}
