// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.web.models.workflow;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class WorkflowTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Workflow()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Workflow.builder().build()).isNotNull();
}

    }
