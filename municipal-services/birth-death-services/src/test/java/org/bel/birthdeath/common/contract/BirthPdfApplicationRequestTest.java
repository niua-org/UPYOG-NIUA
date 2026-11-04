// generated-by: generate_per_class_unit_tests.py
package org.bel.birthdeath.common.contract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BirthPdfApplicationRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BirthPdfApplicationRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BirthPdfApplicationRequest.builder().build()).isNotNull();
}

    }
