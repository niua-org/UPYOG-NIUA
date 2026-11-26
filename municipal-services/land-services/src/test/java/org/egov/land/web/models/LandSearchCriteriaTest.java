// generated-by: generate_per_class_unit_tests.py
package org.egov.land.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class LandSearchCriteriaTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new LandSearchCriteria()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(LandSearchCriteria.builder().build()).isNotNull();
}

    }
