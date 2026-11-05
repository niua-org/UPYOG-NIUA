// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class TradeLicenseRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new TradeLicenseRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(TradeLicenseRequest.builder().build()).isNotNull();
}

    }
