// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AddressTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Address()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Address.builder().build()).isNotNull();
}

    }
