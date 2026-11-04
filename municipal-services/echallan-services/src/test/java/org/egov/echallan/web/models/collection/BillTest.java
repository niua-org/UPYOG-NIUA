// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.web.models.collection;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BillTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Bill()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Bill.builder().build()).isNotNull();
}

    }
