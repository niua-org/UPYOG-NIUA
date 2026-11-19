// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.web.models.idgen;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class IdResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new IdResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(IdResponse.builder().build()).isNotNull();
}

    }
