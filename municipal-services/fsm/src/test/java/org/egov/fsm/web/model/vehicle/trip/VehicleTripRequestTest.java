// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.web.model.vehicle.trip;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class VehicleTripRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new VehicleTripRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(VehicleTripRequest.builder().build()).isNotNull();
}

    }
