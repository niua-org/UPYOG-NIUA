package org.upyog.employee.dasboard.repository.rowMapper;

import org.junit.jupiter.api.Test;
import org.upyog.employee.dasboard.web.models.EmployeeDashboardDetails;
import org.upyog.employee.dasboard.web.models.ModuleName;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeDashboardDetailsRowMapperTest {

    @Test
    void mapRow_readsCountsAndTotalAmount() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.findColumn("Total_Applications_Received")).thenReturn(1);
        when(rs.findColumn("Total_Applications_Approved")).thenReturn(2);
        when(rs.findColumn("Total_Applications_Pending")).thenReturn(3);
        when(rs.findColumn("Total_Amount")).thenReturn(4);
        when(rs.getInt("Total_Applications_Received")).thenReturn(5);
        when(rs.getInt("Total_Applications_Approved")).thenReturn(3);
        when(rs.getInt("Total_Applications_Pending")).thenReturn(2);
        when(rs.getBigDecimal("Total_Amount")).thenReturn(new BigDecimal("100.50"));

        EmployeeDashboardDetailsRowMapper mapper = new EmployeeDashboardDetailsRowMapper(ModuleName.PGR);
        EmployeeDashboardDetails details = mapper.mapRow(rs, 0);

        assertThat(details.getApplicationReceived()).isEqualTo(5);
        assertThat(details.getApplicationApproved()).isEqualTo(3);
        assertThat(details.getApplicationPending()).isEqualTo(2);
        assertThat(details.getTotalAmount()).isEqualByComparingTo("100.50");
    }
}
