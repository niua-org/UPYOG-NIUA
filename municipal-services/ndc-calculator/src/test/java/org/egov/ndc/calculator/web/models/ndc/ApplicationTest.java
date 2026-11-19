// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.calculator.web.models.ndc;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ApplicationTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Application()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Application.builder().build()).isNotNull();
}

    }
