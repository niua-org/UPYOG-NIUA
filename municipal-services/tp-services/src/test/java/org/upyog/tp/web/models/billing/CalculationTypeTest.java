// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.web.models.billing;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class CalculationTypeTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new CalculationType()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(CalculationType.builder().build()).isNotNull();
}

    }
