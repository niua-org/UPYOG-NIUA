// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.web.model.dss;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RequestDateTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new RequestDate()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(RequestDate.builder().build()).isNotNull();
}

    }
