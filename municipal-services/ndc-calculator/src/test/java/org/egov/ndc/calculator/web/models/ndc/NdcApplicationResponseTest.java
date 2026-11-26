// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.calculator.web.models.ndc;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class NdcApplicationResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new NdcApplicationResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(NdcApplicationResponse.builder().build()).isNotNull();
}

    }
