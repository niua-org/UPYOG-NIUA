// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.web.models.events;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class EventRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new EventRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(EventRequest.builder().build()).isNotNull();
}

    }
