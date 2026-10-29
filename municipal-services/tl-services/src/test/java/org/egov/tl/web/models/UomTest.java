// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class UomTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Uom()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Uom.builder().build()).isNotNull();
}

    }
