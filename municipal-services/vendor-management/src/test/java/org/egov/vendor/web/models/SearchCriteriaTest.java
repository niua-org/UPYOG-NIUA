package org.egov.vendor.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCriteriaTest {

    @Test
    void setVendorAdditionalDetailsId_whenRoundTrip_retainsValue() {
        SearchCriteria target = new SearchCriteria();
        target.setVendorAdditionalDetailsId("test");
        assertThat(target.getVendorAdditionalDetailsId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new SearchCriteria()).isNotNull();
    }
}
