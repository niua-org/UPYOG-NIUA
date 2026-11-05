// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class GarbageAccountActionRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new GarbageAccountActionRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(GarbageAccountActionRequest.builder().build()).isNotNull();
}

    }
