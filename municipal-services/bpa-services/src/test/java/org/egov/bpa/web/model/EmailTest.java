// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class EmailTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Email()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Email.builder().build()).isNotNull();
}

    }
