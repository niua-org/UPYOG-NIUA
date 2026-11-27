// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.plantmapping.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PlantMappingRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PlantMappingRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PlantMappingRequest.builder().build()).isNotNull();
}

    }
