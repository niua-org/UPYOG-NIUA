package org.egov.pt.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCriteriaTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        SearchCriteria target = new SearchCriteria();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new SearchCriteria()).isNotNull();
    }
}
