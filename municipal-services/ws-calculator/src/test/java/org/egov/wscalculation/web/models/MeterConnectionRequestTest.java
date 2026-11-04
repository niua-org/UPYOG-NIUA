// generated-by: generate_per_class_unit_tests.py
package org.egov.wscalculation.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class MeterConnectionRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new MeterConnectionRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(MeterConnectionRequest.builder().build()).isNotNull();
}

    }
