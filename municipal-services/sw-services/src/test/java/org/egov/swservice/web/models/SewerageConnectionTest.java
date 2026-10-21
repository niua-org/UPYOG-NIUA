// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class SewerageConnectionTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new SewerageConnection()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(SewerageConnection.builder().build()).isNotNull();
}

    }
