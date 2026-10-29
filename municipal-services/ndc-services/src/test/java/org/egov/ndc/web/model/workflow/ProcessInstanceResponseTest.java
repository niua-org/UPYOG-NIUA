// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.model.workflow;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ProcessInstanceResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ProcessInstanceResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ProcessInstanceResponse.builder().build()).isNotNull();
}

    }
