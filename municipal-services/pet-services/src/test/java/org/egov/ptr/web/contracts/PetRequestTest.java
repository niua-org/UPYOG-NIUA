// generated-by: generate_per_class_unit_tests.py
package org.egov.ptr.web.contracts;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PetRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PetRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PetRequest.builder().build()).isNotNull();
}

    }
