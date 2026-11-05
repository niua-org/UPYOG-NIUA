// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BookingPurposeTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BookingPurpose()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BookingPurpose.builder().build()).isNotNull();
}

    }
