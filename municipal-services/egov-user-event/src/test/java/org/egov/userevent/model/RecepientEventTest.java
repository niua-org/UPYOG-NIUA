// generated-by: generate_per_class_unit_tests.py
package org.egov.userevent.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RecepientEventTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new RecepientEvent()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(RecepientEvent.builder().build()).isNotNull();
}

    }
