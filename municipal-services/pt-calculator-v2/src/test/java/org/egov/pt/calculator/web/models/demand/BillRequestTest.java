// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.web.models.demand;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BillRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BillRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BillRequest.builder().build()).isNotNull();
}

    }
