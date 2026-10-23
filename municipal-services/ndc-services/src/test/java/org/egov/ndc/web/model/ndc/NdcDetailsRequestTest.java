// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.model.ndc;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class NdcDetailsRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new NdcDetailsRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(NdcDetailsRequest.builder().build()).isNotNull();
}

    }
