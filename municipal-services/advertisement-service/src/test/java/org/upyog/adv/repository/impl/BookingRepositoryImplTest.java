// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.kafka.Producer;
import org.upyog.adv.repository.BookingRepository;
import org.upyog.adv.repository.querybuilder.AdvertisementBookingQueryBuilder;
import org.upyog.adv.repository.rowmapper.AdvertisementDraftApplicationRowMapper;
import org.upyog.adv.repository.rowmapper.AdvertisementSlotAvailabilityRowMapper;
import org.upyog.adv.repository.rowmapper.AdvertisementUpdateSlotAvailabilityRowMapper;
import org.upyog.adv.repository.rowmapper.BookingCartDetailRowmapper;
import org.upyog.adv.repository.rowmapper.BookingDetailRowmapper;
import org.upyog.adv.repository.rowmapper.BookingPaymentTimerRowMapper;
import org.upyog.adv.repository.rowmapper.DocumentDetailsRowMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BookingRepositoryImplTest {
    @Mock
    private Producer producer;
    @Mock
    private BookingConfiguration bookingConfiguration;
    @Mock
    private BookingDetailRowmapper bookingRowmapper;
    @Mock
    private BookingCartDetailRowmapper cartDetailRowmapper;
    @Mock
    private DocumentDetailsRowMapper detailsRowMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private AdvertisementBookingQueryBuilder queryBuilder;
    @Mock
    private AdvertisementSlotAvailabilityRowMapper availabilityRowMapper;
    @Mock
    private AdvertisementUpdateSlotAvailabilityRowMapper availabilityUpdateRowMapper;
    @Mock
    private AdvertisementDraftApplicationRowMapper draftApplicationRowMapper;
    @Mock
    private BookingPaymentTimerRowMapper bookingPaymentTimerRowMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private BookingRepository self;

    private BookingRepositoryImpl subject;

    @BeforeEach
    void setUp() {
        subject = new BookingRepositoryImpl(producer, bookingConfiguration, bookingRowmapper, cartDetailRowmapper, detailsRowMapper, jdbcTemplate, queryBuilder, availabilityRowMapper, availabilityUpdateRowMapper, draftApplicationRowMapper, bookingPaymentTimerRowMapper, objectMapper, self);
    }


    @Test
    void saveBooking_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
