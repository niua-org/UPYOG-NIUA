// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models.idgen;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class IdGenerationResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new IdGenerationResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(IdGenerationResponse.builder().build()).isNotNull();
}

    }
