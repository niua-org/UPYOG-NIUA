// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class VenueBookingRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new VenueBookingRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(VenueBookingRequest.builder().build()).isNotNull();
}

    }
