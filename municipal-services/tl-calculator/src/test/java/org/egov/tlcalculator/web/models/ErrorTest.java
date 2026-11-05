// generated-by: generate_per_class_unit_tests.py
package org.egov.tlcalculator.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ErrorTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Error()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Error.builder().build()).isNotNull();
}

    }
