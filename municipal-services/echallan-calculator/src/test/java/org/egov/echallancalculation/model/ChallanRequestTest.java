// generated-by: generate_per_class_unit_tests.py
package org.egov.echallancalculation.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ChallanRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ChallanRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ChallanRequest.builder().build()).isNotNull();
}

    }
