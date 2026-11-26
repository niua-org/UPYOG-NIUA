// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.calculator.web.models.bpa;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BPATest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BPA()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BPA.builder().build()).isNotNull();
}

    }
