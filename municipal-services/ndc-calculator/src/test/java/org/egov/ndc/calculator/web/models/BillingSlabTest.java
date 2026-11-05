// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.calculator.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BillingSlabTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BillingSlab()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BillingSlab.builder().build()).isNotNull();
}

    }
