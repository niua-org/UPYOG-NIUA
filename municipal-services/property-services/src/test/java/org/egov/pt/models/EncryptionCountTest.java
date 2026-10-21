// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class EncryptionCountTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new EncryptionCount()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(EncryptionCount.builder().build()).isNotNull();
}

    }
