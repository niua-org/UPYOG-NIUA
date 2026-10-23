// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class TestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new org.egov.pqm.web.model.Test()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(org.egov.pqm.web.model.Test.builder().build()).isNotNull();
}

    }
