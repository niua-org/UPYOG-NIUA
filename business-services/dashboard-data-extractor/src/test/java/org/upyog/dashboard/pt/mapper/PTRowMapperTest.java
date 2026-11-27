package org.upyog.dashboard.pt.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.upyog.dashboard.pt.constants.PTDatabaseConstants;
import org.upyog.dashboard.pt.model.RawPtCollection;
import org.upyog.dashboard.pt.model.RawPtMetric;

class PTRowMapperTest {

    @Test
    @DisplayName("COMBINED_ROW_MAPPER maps PT metric columns")
    void combinedRowMapper_mapsPtMetric() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.getString(PTDatabaseConstants.TENANT_ID)).thenReturn("pg.citya");
        when(resultSet.getString(PTDatabaseConstants.PROPERTIES_REGISTERED_JSON)).thenReturn("[]");
        when(resultSet.getString(PTDatabaseConstants.ASSESSED_PROPERTIES_JSON)).thenReturn("[]");
        when(resultSet.getString(PTDatabaseConstants.MOVED_APPLICATIONS_JSON)).thenReturn("[]");
        when(resultSet.getInt(Mockito.anyString())).thenReturn(5);
        when(resultSet.wasNull()).thenReturn(false);

        RawPtMetric metric = PTRowmapper.COMBINED_ROW_MAPPER.mapRow(resultSet, 1);

        assertThat(metric.getTenantid()).isEqualTo("pg.citya");
        assertThat(metric.getAssessments()).isEqualTo(5);
    }

    @Test
    @DisplayName("COLLECTION_ROW_MAPPER maps PT collection columns including nullable amount")
    void collectionRowMapper_mapsPtCollection() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.getString(PTDatabaseConstants.TENANT_ID)).thenReturn("pg.citya");
        when(resultSet.getString(PTDatabaseConstants.USAGE_CATEGORY)).thenReturn("RESIDENTIAL");
        when(resultSet.getString(PTDatabaseConstants.PAYMENT_MODE)).thenReturn("CASH");
        when(resultSet.getString(PTDatabaseConstants.PAYMENT_ID)).thenReturn("pay-1");
        when(resultSet.getString(PTDatabaseConstants.TAX_HEAD_CODE)).thenReturn("PT_TAX");
        when(resultSet.getDouble(PTDatabaseConstants.TAX_HEAD_AMOUNT)).thenReturn(250.5);
        when(resultSet.wasNull()).thenReturn(false);

        RawPtCollection collection = PTRowmapper.COLLECTION_ROW_MAPPER.mapRow(resultSet, 1);

        assertThat(collection.getTenantid()).isEqualTo("pg.citya");
        assertThat(collection.getTaxHeadAmount()).isEqualTo(250.5);
    }
}
