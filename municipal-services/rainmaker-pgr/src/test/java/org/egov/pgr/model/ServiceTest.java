// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ServiceTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Service()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Service.builder().build()).isNotNull();
}

    }
