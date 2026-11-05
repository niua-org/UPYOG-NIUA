// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.calculator.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BillDetailTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BillDetail()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BillDetail.builder().build()).isNotNull();
}

    }
