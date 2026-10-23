// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.repository.querybuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdvertisementBookingQueryBuilderTest {
    @Mock
    private BookingConfiguration bookingConfiguration;

    private AdvertisementBookingQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new AdvertisementBookingQueryBuilder(bookingConfiguration);
    }


    @Test
    void getPaymentTimerInsertQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
