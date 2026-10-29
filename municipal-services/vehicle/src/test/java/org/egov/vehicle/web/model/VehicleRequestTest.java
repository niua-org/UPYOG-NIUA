// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class VehicleRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new VehicleRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(VehicleRequest.builder().build()).isNotNull();
}

    }
