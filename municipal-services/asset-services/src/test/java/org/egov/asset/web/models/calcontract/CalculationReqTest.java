// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.models.calcontract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class CalculationReqTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new CalculationReq()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(CalculationReq.builder().build()).isNotNull();
}

    }
