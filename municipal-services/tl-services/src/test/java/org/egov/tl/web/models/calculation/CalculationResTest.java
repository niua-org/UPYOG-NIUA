// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.web.models.calculation;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class CalculationResTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new CalculationRes()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(CalculationRes.builder().build()).isNotNull();
}

    }
