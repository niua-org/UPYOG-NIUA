// generated-by: generate_per_class_unit_tests.py
package org.upyog.rs.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ResponseInfoTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ResponseInfo()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ResponseInfo.builder().build()).isNotNull();
}

    }
