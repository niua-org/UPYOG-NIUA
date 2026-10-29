// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.calculator.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class DocumentTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Document()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Document.builder().build()).isNotNull();
}

    }
