// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.web.models.events;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class EventDetailsTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new EventDetails()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(EventDetails.builder().build()).isNotNull();
}

    }
