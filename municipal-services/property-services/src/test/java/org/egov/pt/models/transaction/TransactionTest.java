// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models.transaction;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class TransactionTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Transaction()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Transaction.builder().build()).isNotNull();
}

    }
