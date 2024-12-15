package org.upyog.repository.impl;

import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.upyog.repository.CommonDetailRepositoryInterface;
import org.upyog.repository.rowmapper.AdvertisementDetailsRowMapper;
import org.upyog.web.models.CommonDetails;

@Repository
public class AdvertisementDetailRepository implements CommonDetailRepositoryInterface {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public String getModuleName() {
		return "advertisement";
	}

	@Override
	public CommonDetails findApplicationDetails(String applicationNumber) {
		 String query = "SELECT "
		 		+ "    eab.booking_no as applicationnumber, "
		 		+ "    MIN(eacd.booking_date) AS fromdate, "
		 		+ "    MAX(eacd.booking_date) AS todate, "
		 		+ "    eacd.status as status, "
		 		+ "    eacd.location as fulladdress "
		 		+ "FROM eg_adv_cart_detail eacd "
		 		+ "JOIN eg_adv_booking_detail eab "
		 		+ "ON eacd.booking_id = eab.booking_id "
		 		+ "WHERE eab.booking_no = ? "
		 		+ "GROUP BY eab.booking_no, eacd.status, eacd.location";
			try {
				return jdbcTemplate.queryForObject(query, new Object[] { applicationNumber },
						new AdvertisementDetailsRowMapper());
			} catch (Exception e) {
				throw new CustomException("No results found for application number: " + applicationNumber, "NO_APPLICATION_FOUND" );
			}
	}

}
