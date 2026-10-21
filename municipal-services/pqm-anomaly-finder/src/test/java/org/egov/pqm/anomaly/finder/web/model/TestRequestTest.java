// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.anomaly.finder.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class TestRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new TestRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(TestRequest.builder().build()).isNotNull();
}

    }
