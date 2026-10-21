// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class StreetVendingRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new StreetVendingRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(StreetVendingRequest.builder().build()).isNotNull();
}

    }
