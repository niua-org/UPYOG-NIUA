// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.web.models.collection;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PaymentTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Payment()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Payment.builder().build()).isNotNull();
}

    }
