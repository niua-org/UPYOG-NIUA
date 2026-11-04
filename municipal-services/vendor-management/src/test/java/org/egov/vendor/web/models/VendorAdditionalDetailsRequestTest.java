// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class VendorAdditionalDetailsRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new VendorAdditionalDetailsRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(VendorAdditionalDetailsRequest.builder().build()).isNotNull();
}

    }
