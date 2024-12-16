package org.upyog.repository.impl;

import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.upyog.repository.CommonDetailRepositoryInterface;
import org.upyog.repository.rowmapper.StreetVendingDetailsRowMapper;
import org.upyog.web.models.CommonDetails;

@Repository
public class StreetVendingDetailRepository implements CommonDetailRepositoryInterface{

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public String getModuleName() {
		return "street-vending";
	}

	@Override
	public CommonDetails findApplicationDetails(String applicationNumber) {
		 String query = "SELECT sv.APPLICATION_NO as applicationnumber, "
		 		+ "	SV.approval_date as approvaldate,"
		 		+ "	SV.APPLICATION_STATUS as status, "
		 		+ "	CONCAT_WS(', ', "
		 		+ "        COALESCE(ADDRESS.house_no, ''), "
		 		+ "        COALESCE(ADDRESS.address_line_1, ''), "
		 		+ "        COALESCE(ADDRESS.locality, ''), "
		 		+ "        COALESCE(ADDRESS.city, '') "
		 		+ "    ) AS fulladdress "
		 		+ "FROM EG_SV_STREET_VENDING_DETAIL SV "
		 		+ "LEFT JOIN EG_SV_VENDOR_DETAIL VENDOR ON SV.APPLICATION_ID = VENDOR.APPLICATION_ID "
		 		+ "LEFT JOIN EG_SV_ADDRESS_DETAIL ADDRESS ON VENDOR.ID = ADDRESS.VENDOR_ID "
		 		+ "WHERE APPLICATION_NO = ? LIMIT 1";
			try {
				return jdbcTemplate.queryForObject(query, new Object[] { applicationNumber },
						new StreetVendingDetailsRowMapper());
			} catch (Exception e) {
				throw new CustomException("No results found for application number: " + applicationNumber,
						"NO_APPLICATION_FOUND");
			}
	}

}
