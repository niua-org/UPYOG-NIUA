package org.upyog.chb.repository.impl;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.kafka.producer.Producer;
import org.upyog.chb.repository.querybuilder.CommunityHallBookingQueryBuilder;
import org.upyog.chb.repository.rowmapper.CommunityHallBookingRowmapper;
import org.upyog.chb.web.models.CommunityHallBookingDetail;
import org.upyog.chb.web.models.CommunityHallBookingRequest;
import org.upyog.chb.web.models.CommunityHallBookingSearchCriteria;

public class CommunityHallBookingRepositoryImplTest {

    @Mock
    private Producer producer;

    @Mock
    private CommunityHallBookingConfiguration bookingConfiguration;

    @Mock
    private CommunityHallBookingQueryBuilder queryBuilder;

    @Mock
    private CommunityHallBookingRowmapper bookingRowmapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CommunityHallBookingRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveCommunityHallBookingWithNullApplication() {
        // Arrange
        CommunityHallBookingRequest bookingRequest = mock(CommunityHallBookingRequest.class);
        when(bookingRequest.getHallsBookingApplication()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> repository.saveCommunityHallBooking(bookingRequest));
    }

    @Test
    void testGetBookingDetails() {
        // Arrange
        CommunityHallBookingSearchCriteria criteria = new CommunityHallBookingSearchCriteria();
        List<Object> preparedStmtList = new ArrayList<>();
        String query = "SELECT * FROM bookings";
        when(queryBuilder.getCommunityHallBookingSearchQuery(criteria, preparedStmtList)).thenReturn(query);
        when(jdbcTemplate.query(eq(query), any(Object[].class), eq(bookingRowmapper)))
            .thenReturn(new ArrayList<>());

        // Act
        List<CommunityHallBookingDetail> result = repository.getBookingDetails(criteria);

        // Assert
        verify(jdbcTemplate).query(eq(query), any(Object[].class), eq(bookingRowmapper));
        assert result.isEmpty();
    }

    @Test
    void testUpdateBookingWithNullApplication() {
        // Arrange
        CommunityHallBookingRequest bookingRequest = mock(CommunityHallBookingRequest.class);
        when(bookingRequest.getHallsBookingApplication()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> repository.updateBooking(bookingRequest));
    }
}