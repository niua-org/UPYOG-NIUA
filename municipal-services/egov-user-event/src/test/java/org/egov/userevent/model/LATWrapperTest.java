// generated-by: generate_per_class_unit_tests.py
package org.egov.userevent.model;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class LATWrapperTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new LATWrapper()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(LATWrapper.builder().build()).isNotNull();
}

    }
