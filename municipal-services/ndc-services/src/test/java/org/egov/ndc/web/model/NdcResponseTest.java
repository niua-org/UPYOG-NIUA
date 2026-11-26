// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class NdcResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new NdcResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(NdcResponse.builder().build()).isNotNull();
}

    }
