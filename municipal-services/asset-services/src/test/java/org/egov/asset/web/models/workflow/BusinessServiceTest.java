// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.models.workflow;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BusinessServiceTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BusinessService()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BusinessService.builder().build()).isNotNull();
}

    }
