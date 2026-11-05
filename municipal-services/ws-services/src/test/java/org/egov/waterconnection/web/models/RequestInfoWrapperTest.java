// generated-by: generate_per_class_unit_tests.py
package org.egov.waterconnection.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RequestInfoWrapperTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new RequestInfoWrapper()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(RequestInfoWrapper.builder().build()).isNotNull();
}

    }
