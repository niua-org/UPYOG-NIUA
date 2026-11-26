package org.egov.inbox.web.model.elasticsearch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InboxElasticSearchCriteriaTest {

    @Test
    void setIndexKey_whenRoundTrip_retainsValue() {
        InboxElasticSearchCriteria target = new InboxElasticSearchCriteria();
        target.setIndexKey("test");
        assertThat(target.getIndexKey()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new InboxElasticSearchCriteria()).isNotNull();
    }
}
