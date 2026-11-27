package org.egov.inbox.web.model.V2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndexSearchCriteriaTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        IndexSearchCriteria target = new IndexSearchCriteria();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new IndexSearchCriteria()).isNotNull();
    }
}
