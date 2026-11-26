// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model.mdms;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PlantConfigTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PlantConfig()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PlantConfig.builder().build()).isNotNull();
}

    }
