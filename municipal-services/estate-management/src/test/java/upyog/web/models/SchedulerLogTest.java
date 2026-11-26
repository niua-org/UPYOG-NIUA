// generated-by: generate_per_class_unit_tests.py
package upyog.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class SchedulerLogTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new SchedulerLog()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(SchedulerLog.builder().build()).isNotNull();
}

    }
