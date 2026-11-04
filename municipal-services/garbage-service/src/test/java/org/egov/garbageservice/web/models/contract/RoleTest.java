// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.web.models.contract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RoleTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Role()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Role.builder().build()).isNotNull();
}

    }
