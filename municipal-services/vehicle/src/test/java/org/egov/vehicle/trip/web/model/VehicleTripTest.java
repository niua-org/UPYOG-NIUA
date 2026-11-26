// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.trip.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class VehicleTripTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new VehicleTrip()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(VehicleTrip.builder().build()).isNotNull();
}

    }
