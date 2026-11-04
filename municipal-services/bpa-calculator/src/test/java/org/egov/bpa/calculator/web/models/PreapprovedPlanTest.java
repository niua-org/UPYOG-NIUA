// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.calculator.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PreapprovedPlanTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PreapprovedPlan()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PreapprovedPlan.builder().build()).isNotNull();
}

    }
