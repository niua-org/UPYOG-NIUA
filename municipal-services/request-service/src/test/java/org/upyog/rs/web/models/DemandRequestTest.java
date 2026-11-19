// generated-by: generate_per_class_unit_tests.py
package org.upyog.rs.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class DemandRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new DemandRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(DemandRequest.builder().build()).isNotNull();
}

    }
