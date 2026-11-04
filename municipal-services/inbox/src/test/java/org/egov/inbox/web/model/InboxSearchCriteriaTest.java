package org.egov.inbox.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InboxSearchCriteriaTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        InboxSearchCriteria target = new InboxSearchCriteria();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new InboxSearchCriteria()).isNotNull();
    }
}
