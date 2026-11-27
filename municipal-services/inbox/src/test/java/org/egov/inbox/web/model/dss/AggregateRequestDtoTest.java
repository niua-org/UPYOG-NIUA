// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.web.model.dss;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AggregateRequestDtoTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new AggregateRequestDto()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(AggregateRequestDto.builder().build()).isNotNull();
}

    }
