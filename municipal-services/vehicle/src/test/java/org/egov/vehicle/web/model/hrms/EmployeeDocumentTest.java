// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.web.model.hrms;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class EmployeeDocumentTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new EmployeeDocument()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(EmployeeDocument.builder().build()).isNotNull();
}

    }
