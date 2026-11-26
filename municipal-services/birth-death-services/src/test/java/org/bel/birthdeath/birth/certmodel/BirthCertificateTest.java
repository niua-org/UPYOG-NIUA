// generated-by: generate_per_class_unit_tests.py
package org.bel.birthdeath.birth.certmodel;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BirthCertificateTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BirthCertificate()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BirthCertificate.builder().build()).isNotNull();
}

    }
