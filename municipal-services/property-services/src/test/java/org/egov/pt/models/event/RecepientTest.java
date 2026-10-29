// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models.event;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RecepientTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Recepient()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Recepient.builder().build()).isNotNull();
}

    }
