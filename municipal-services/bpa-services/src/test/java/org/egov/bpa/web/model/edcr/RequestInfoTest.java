// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.web.model.edcr;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RequestInfoTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new RequestInfo()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(RequestInfo.builder().build()).isNotNull();
}

    }
