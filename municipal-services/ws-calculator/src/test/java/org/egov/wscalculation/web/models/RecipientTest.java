// generated-by: generate_per_class_unit_tests.py
package org.egov.wscalculation.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RecipientTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Recipient()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Recipient.builder().build()).isNotNull();
}

    }
