package org.upyog.repository.impl;

import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.upyog.repository.CommonDetailRepositoryInterface;
import org.upyog.repository.rowmapper.CommunityHallBookingDetailsRowMapper;
import org.upyog.web.models.CommonDetails;

@Repository
public class CommunityHallDetailRepository implements CommonDetailRepositoryInterface {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public String getModuleName() {
		return "community-hall";
	}

	@Override
	public CommonDetails findApplicationDetails(String applicationNumber) {
		 String query = "SELECT "
		 		+ "    ecb.booking_no as applicationnumber, "
		 		+ "    MIN(ecsd.booking_date) AS fromdate, "
		 		+ "    MAX(ecsd.booking_date) AS todate, "
		 		+ "    ecsd.status as status, "
		 		+ "    ecb.community_hall_code as fulladdress "
		 		+ "FROM eg_chb_slot_detail ecsd "
		 		+ "JOIN eg_chb_booking_detail ecb "
		 		+ "ON ecsd.booking_id = ecb.booking_id "
		 		+ "WHERE ecb.booking_no = ? "
		 		+ "GROUP BY ecb.booking_no, ecsd.status, ecsd.hall_code, ecb.community_hall_code";
			try {
				return jdbcTemplate.queryForObject(query, new Object[] { applicationNumber },
						new CommunityHallBookingDetailsRowMapper());
			} catch (Exception e) {
				throw new CustomException("No results found for application number: " + applicationNumber,
						"NO_APPLICATION_FOUND");
			}
	}

}
