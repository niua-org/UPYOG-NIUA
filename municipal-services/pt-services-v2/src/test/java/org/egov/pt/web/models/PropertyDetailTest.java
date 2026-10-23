// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PropertyDetailTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PropertyDetail()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PropertyDetail.builder().build()).isNotNull();
}

    }
