// generated-by: generate_per_class_unit_tests.py
package org.bel.birthdeath.birth.certmodel;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BirthCertApplnTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BirthCertAppln()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BirthCertAppln.builder().build()).isNotNull();
}

    }
