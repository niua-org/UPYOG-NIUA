package org.upyog.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.upyog.util.CommonDetailUtil;
import org.upyog.web.models.CommonDetails;

public class StreetVendingDetailsRowMapper implements RowMapper<CommonDetails> {

	@Override
	public CommonDetails mapRow(ResultSet rs, int rowNum) throws SQLException {

		String validFromString = rs.getString("approvaldate");
		String validToString = "NA";
		if (!validFromString.equals("0")) {
			validFromString = CommonDetailUtil.convertToFormattedDate(validFromString, "dd-MM-YYYY");
			validToString = CommonDetailUtil.addOneYearToEpoch(validFromString);
		} else {
			validFromString = "NA";
		}

		return CommonDetails.builder().applicationNumber(rs.getString("applicationnumber")).fromDate(validFromString)
				.toDate(validToString).address(rs.getString("fulladdress")).status(rs.getString("status")).build();
	}
}
