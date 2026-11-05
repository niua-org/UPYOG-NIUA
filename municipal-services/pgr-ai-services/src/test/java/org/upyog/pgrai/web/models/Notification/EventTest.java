// generated-by: generate_per_class_unit_tests.py
package org.upyog.pgrai.web.models.notification;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class EventTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Event()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Event.builder().build()).isNotNull();
}

    }
