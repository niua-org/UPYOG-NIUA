// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class FSMTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new FSM()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(FSM.builder().build()).isNotNull();
}

    }
