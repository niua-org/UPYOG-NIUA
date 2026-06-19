package org.upyog.chb.kafka.consumer;

import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.KafkaHeaders;
import org.upyog.chb.enums.BookingStatusEnum;
import org.upyog.chb.service.CHBNotificationService;
import org.upyog.chb.web.models.VenueBookingDetail;
import org.upyog.chb.web.models.VenueBookingRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

class NotificationConsumerTest {

    @Mock
    private CHBNotificationService notificationService;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testListenWithValidRecord() throws Exception {
        // Arrange
        HashMap<String, Object> record = new HashMap<>();
        record.put("key", "value");

        VenueBookingRequest bookingRequest = new VenueBookingRequest();
        VenueBookingDetail bookingDetail = new VenueBookingDetail();
        bookingDetail.setBookingStatus("APPROVED");
        bookingRequest.setHallsBookingApplication(bookingDetail);

        when(mapper.convertValue(record, VenueBookingRequest.class)).thenReturn(bookingRequest);

        // Act
        notificationConsumer.listen(record, "test-topic");

        // Assert
        verify(notificationService).process(bookingRequest, "APPROVED");
    }

    @Test
    void testListenWithPendingForPaymentStatus() throws Exception {
        // Arrange
        HashMap<String, Object> record = new HashMap<>();
        record.put("key", "value");

        VenueBookingRequest bookingRequest = new VenueBookingRequest();
        VenueBookingDetail bookingDetail = new VenueBookingDetail();
        bookingDetail.setBookingStatus(BookingStatusEnum.PENDING_FOR_PAYMENT.toString());
        bookingRequest.setHallsBookingApplication(bookingDetail);

        when(mapper.convertValue(record, VenueBookingRequest.class)).thenReturn(bookingRequest);

        // Act
        notificationConsumer.listen(record, "test-topic");

        // Assert
        verify(notificationService, never()).process(any(), any());
    }

    @Test
    void testListenWithInvalidRecord() {
        // Arrange
        HashMap<String, Object> record = new HashMap<>();
        record.put("key", "value");

        when(mapper.convertValue(record, VenueBookingRequest.class)).thenThrow(new RuntimeException("Error"));

        // Act
        notificationConsumer.listen(record, "test-topic");

        // Assert
        verify(notificationService, never()).process(any(), any());
    }
}