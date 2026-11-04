// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models.idgen;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class IdGenerationRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new IdGenerationRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(IdGenerationRequest.builder().build()).isNotNull();
}

    }
