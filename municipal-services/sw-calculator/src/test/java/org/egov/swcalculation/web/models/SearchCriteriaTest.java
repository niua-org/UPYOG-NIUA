// generated-by: generate_per_class_unit_tests.py
package org.egov.swcalculation.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class SearchCriteriaTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new SearchCriteria()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(SearchCriteria.builder().build()).isNotNull();
}

    }
