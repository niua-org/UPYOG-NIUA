// generated-by: generate_per_class_unit_tests.py
package org.egov.ptr.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PetApplicationSearchCriteriaTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new PetApplicationSearchCriteria()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(PetApplicationSearchCriteria.builder().build()).isNotNull();
}

    }
