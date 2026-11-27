// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class InboxTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Inbox()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Inbox.builder().build()).isNotNull();
}

    }
