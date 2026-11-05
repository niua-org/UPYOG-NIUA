// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class DraftSearchCriteriaTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new DraftSearchCriteria()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(DraftSearchCriteria.builder().build()).isNotNull();
}

    }
