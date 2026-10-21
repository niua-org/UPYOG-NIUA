// generated-by: generate_per_class_unit_tests.py
package org.egov.swcalculation.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class TaxPeriodRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new TaxPeriodRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(TaxPeriodRequest.builder().build()).isNotNull();
}

    }
