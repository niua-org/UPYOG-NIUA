// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.web.model.dss;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AggregationRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new AggregationRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(AggregationRequest.builder().build()).isNotNull();
}

    }
