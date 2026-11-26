// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.web.contracts;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class FuzzySearchCriteriaTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new FuzzySearchCriteria()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(FuzzySearchCriteria.builder().build()).isNotNull();
}

    }
