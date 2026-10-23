package org.egov.inbox.web.model.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessServiceSearchCriteriaTest {

    @Test
    void setTenantIds_whenRoundTrip_retainsValue() {
        BusinessServiceSearchCriteria target = new BusinessServiceSearchCriteria();
        target.setTenantIds(java.util.List.of("test"));
        assertThat(target.getTenantIds()).containsExactly("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new BusinessServiceSearchCriteria()).isNotNull();
    }
}
