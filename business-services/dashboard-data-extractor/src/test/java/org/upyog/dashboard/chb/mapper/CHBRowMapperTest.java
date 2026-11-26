package org.upyog.dashboard.chb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.upyog.dashboard.chb.constants.CHBDatabaseConstants;
import org.upyog.dashboard.chb.model.RawChbMetric;

class CHBRowMapperTest {

    @Test
    @DisplayName("COMBINED_ROW_MAPPER maps ResultSet columns to RawChbMetric")
    void combinedRowMapper_mapsColumns() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.getString(CHBDatabaseConstants.TENANT_ID)).thenReturn("pg.citya");
        when(resultSet.getString(CHBDatabaseConstants.BOOKINGS_JSON)).thenReturn("[]");
        when(resultSet.getString(CHBDatabaseConstants.CREATED_BY_LIST_JSON)).thenReturn("[]");
        when(resultSet.getInt(Mockito.anyString())).thenAnswer(invocation -> {
            String column = invocation.getArgument(0);
            if (CHBDatabaseConstants.TOTAL_ACTIVE_VENUE_AVAILABLE.equals(column)) {
                return 3;
            }
            if (CHBDatabaseConstants.TOTAL_APPLICATION_RECEIVED.equals(column)) {
                return 10;
            }
            if (CHBDatabaseConstants.TOTAL_COLLECTIONS.equals(column)) {
                return 100;
            }
            if (CHBDatabaseConstants.NO_SHOW_BOOKINGS.equals(column)) {
                return 1;
            }
            return 0;
        });
        when(resultSet.wasNull()).thenReturn(false);

        RawChbMetric metric = CHBRowMapper.COMBINED_ROW_MAPPER.mapRow(resultSet, 1);

        assertThat(metric.getTenantid()).isEqualTo("pg.citya");
        assertThat(metric.getTotalActiveVenueAvailable()).isEqualTo(3);
        assertThat(metric.getTotalApplicationReceived()).isEqualTo(10);
        assertThat(metric.getBookingsJson()).isEqualTo("[]");
    }
}
