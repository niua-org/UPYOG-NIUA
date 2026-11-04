// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class GarbageSpecificationTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new GarbageSpecification()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(GarbageSpecification.builder().build()).isNotNull();
}

    }
