// generated-by: generate_per_class_unit_tests.py
package org.bel.birthdeath.death.certmodel;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class DeathCertApplnTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new DeathCertAppln()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(DeathCertAppln.builder().build()).isNotNull();
}

    }
