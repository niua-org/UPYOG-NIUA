// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.web.contracts;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PropertyResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PropertyResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PropertyResponse.builder().build()).isNotNull();
}

    }
