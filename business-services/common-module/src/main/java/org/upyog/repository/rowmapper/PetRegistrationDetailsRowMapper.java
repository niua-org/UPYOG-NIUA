package org.upyog.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.upyog.web.models.CommonDetails;

public class PetRegistrationDetailsRowMapper implements RowMapper<CommonDetails>{

	@Override
    public CommonDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
        return CommonDetails.builder()
                .applicationNumber(rs.getString("applicationnumber"))
                .createdTime(rs.getLong("createdtime"))
                .address(rs.getString("fulladdress"))
                .build();
    }
}
