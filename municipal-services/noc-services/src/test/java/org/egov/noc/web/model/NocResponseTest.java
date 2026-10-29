// generated-by: generate_per_class_unit_tests.py
package org.egov.noc.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class NocResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new NocResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(NocResponse.builder().build()).isNotNull();
}

    }
