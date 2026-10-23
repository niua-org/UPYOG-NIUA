// generated-by: generate_per_class_unit_tests.py
package org.egov.userevent.web.contract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ActionItemTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ActionItem()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ActionItem.builder().build()).isNotNull();
}

    }
