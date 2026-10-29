// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class QualityCriteriaTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new QualityCriteria()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(QualityCriteria.builder().build()).isNotNull();
}

    }
