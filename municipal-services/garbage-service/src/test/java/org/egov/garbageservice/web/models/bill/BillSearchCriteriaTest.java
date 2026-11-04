// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.web.models.bill;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BillSearchCriteriaTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BillSearchCriteria()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BillSearchCriteria.builder().build()).isNotNull();
}

    }
