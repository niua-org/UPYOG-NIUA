// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model.idgen;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class IdRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new IdRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(IdRequest.builder().build()).isNotNull();
}

    }
