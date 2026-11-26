// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor.driver.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class DriverRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new DriverRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(DriverRequest.builder().build()).isNotNull();
}

    }
