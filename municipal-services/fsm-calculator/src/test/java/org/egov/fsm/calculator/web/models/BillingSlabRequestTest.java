// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.calculator.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BillingSlabRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BillingSlabRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BillingSlabRequest.builder().build()).isNotNull();
}

    }
