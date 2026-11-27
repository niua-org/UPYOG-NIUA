// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model.plant.user;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PlantUserRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PlantUserRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PlantUserRequest.builder().build()).isNotNull();
}

    }
