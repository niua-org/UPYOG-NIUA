// generated-by: generate_per_class_unit_tests.py
package upyog.notification.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class NotificationConfigTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new NotificationConfig()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(NotificationConfig.builder().build()).isNotNull();
}

    }
