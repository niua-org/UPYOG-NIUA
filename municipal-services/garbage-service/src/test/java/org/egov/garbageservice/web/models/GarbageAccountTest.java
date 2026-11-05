// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class GarbageAccountTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new GarbageAccount()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(GarbageAccount.builder().build()).isNotNull();
}

    }
