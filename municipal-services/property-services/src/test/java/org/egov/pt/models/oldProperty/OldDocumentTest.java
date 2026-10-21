// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models.oldProperty;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class OldDocumentTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new OldDocument()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(OldDocument.builder().build()).isNotNull();
}

    }
