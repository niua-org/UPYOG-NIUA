// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.web.models.pgrV1;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ServiceDefTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ServiceDef()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ServiceDef.builder().build()).isNotNull();
}

    }
