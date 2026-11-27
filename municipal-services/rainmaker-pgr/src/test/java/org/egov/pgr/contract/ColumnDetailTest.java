// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.contract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ColumnDetailTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ColumnDetail()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ColumnDetail.builder().build()).isNotNull();
}

    }
