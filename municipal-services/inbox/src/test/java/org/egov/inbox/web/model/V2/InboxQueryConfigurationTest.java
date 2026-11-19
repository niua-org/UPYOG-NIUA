// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.web.model.V2;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class InboxQueryConfigurationTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new InboxQueryConfiguration()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(InboxQueryConfiguration.builder().build()).isNotNull();
}

    }
