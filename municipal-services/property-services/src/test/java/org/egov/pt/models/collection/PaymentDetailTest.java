// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models.collection;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PaymentDetailTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PaymentDetail()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PaymentDetail.builder().build()).isNotNull();
}

    }
