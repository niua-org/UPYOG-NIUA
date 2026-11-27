// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.web.models.workflow;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ProcessInstanceTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ProcessInstance()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ProcessInstance.builder().build()).isNotNull();
}

    }
