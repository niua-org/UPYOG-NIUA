// generated-by: generate_per_class_unit_tests.py
package org.egov.tlcalculator.web.models.tradelicense;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class TradeLicenseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new TradeLicense()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(TradeLicense.builder().build()).isNotNull();
}

    }
