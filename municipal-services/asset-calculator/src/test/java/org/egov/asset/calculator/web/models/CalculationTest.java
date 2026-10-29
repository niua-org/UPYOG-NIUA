// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class CalculationTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Calculation()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Calculation.builder().build()).isNotNull();
}

    }
