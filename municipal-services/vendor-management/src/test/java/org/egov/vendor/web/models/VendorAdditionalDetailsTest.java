package org.egov.vendor.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VendorAdditionalDetailsTest {

    @Test
    void setVendorAdditionalDetailsId_whenRoundTrip_retainsValue() {
        VendorAdditionalDetails target = new VendorAdditionalDetails();
        target.setVendorAdditionalDetailsId("test");
        assertThat(target.getVendorAdditionalDetailsId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new VendorAdditionalDetails()).isNotNull();
    }
}
