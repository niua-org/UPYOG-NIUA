// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.web.models.workflow;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class StateTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new State()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(State.builder().build()).isNotNull();
}

    }
