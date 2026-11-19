// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.web.model.NOC;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class NocRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new NocRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(NocRequest.builder().build()).isNotNull();
}

    }
