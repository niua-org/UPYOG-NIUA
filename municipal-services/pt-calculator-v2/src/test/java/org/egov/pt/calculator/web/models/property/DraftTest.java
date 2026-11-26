// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.web.models.property;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class DraftTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Draft()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Draft.builder().build()).isNotNull();
}

    }
