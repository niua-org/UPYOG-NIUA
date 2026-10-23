// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models.transaction;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class TransactionRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new TransactionRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(TransactionRequest.builder().build()).isNotNull();
}

    }
