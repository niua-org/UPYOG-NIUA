// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class VendorPaymentScheduleRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new VendorPaymentScheduleRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(VendorPaymentScheduleRequest.builder().build()).isNotNull();
}

    }
