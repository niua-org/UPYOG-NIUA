package org.upyog.dashboard.pgr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.upyog.dashboard.pgr.constants.PGRDatabaseConstants;
import org.upyog.dashboard.pgr.model.RawPgrMetric;

class PGRRowMapperTest {

    @Test
    @DisplayName("COMBINED_ROW_MAPPER maps ResultSet columns to RawPgrMetric")
    void combinedRowMapper_mapsColumns() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.getString(Mockito.anyString())).thenReturn("json-value");
        when(resultSet.getInt(PGRDatabaseConstants.UNIQUE_CITIZENS)).thenReturn(42);
        when(resultSet.wasNull()).thenReturn(false);

        RawPgrMetric metric = PGRRowMapper.COMBINED_ROW_MAPPER.mapRow(resultSet, 1);

        assertThat(metric.getTenantid()).isEqualTo("json-value");
        assertThat(metric.getUniquecitizens()).isEqualTo(42);
        assertThat(metric.getSlaachievementjson()).isEqualTo("json-value");
    }

    @Test
    @DisplayName("COMBINED_ROW_MAPPER maps NULL integer column to null")
    void combinedRowMapper_nullInteger() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.getString(Mockito.anyString())).thenReturn("pg.citya");
        when(resultSet.getInt(PGRDatabaseConstants.UNIQUE_CITIZENS)).thenReturn(0);
        when(resultSet.wasNull()).thenReturn(true);

        RawPgrMetric metric = PGRRowMapper.COMBINED_ROW_MAPPER.mapRow(resultSet, 1);

        assertThat(metric.getUniquecitizens()).isNull();
    }
}
