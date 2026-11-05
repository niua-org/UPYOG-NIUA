// generated-by: generate_per_class_unit_tests.py
package org.bel.birthdeath.common.contract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BirthResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BirthResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BirthResponse.builder().build()).isNotNull();
}

    }
