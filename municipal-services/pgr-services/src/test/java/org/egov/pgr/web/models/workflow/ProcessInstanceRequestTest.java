// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.web.models.workflow;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ProcessInstanceRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ProcessInstanceRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ProcessInstanceRequest.builder().build()).isNotNull();
}

    }
