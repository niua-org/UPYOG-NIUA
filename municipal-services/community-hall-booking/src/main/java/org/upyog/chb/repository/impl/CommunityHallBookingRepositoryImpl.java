package org.upyog.chb.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.kafka.producer.Producer;
import org.upyog.chb.repository.CommunityHallBookingRepository;
import org.upyog.chb.repository.querybuilder.CommunityHallBookingQueryBuilder;
import org.upyog.chb.repository.rowmapper.BankDetailsRowMapper;
import org.upyog.chb.repository.rowmapper.CommunityHallBookingRowmapper;
import org.upyog.chb.repository.rowmapper.DocumentDetailsRowMapper;
import org.upyog.chb.util.CommunityHallBookingUtil;
import org.upyog.chb.web.models.BankDetails;
import org.upyog.chb.web.models.CommunityHallBokingInitDetails;
import org.upyog.chb.web.models.CommunityHallBookingDetail;
import org.upyog.chb.web.models.CommunityHallBookingRequest;
import org.upyog.chb.web.models.CommunityHallBookingRequestInit;
import org.upyog.chb.web.models.CommunityHallBookingSearchCriteria;
import org.upyog.chb.web.models.DocumentDetails;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class CommunityHallBookingRepositoryImpl implements CommunityHallBookingRepository {

	@Autowired
	private Producer producer;
	@Autowired
	private CommunityHallBookingConfiguration bookingConfiguration;
	@Autowired
	private CommunityHallBookingQueryBuilder queryBuilder;
	@Autowired
	private CommunityHallBookingRowmapper bookingRowmapper;
	@Autowired
	private BankDetailsRowMapper bankDetailsRowMapper;
	@Autowired
	private DocumentDetailsRowMapper detailsRowMapper;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void saveCommunityHallBooking(CommunityHallBookingRequest bookingRequest) {
		producer.push(bookingConfiguration.getCommunityHallBookingSaveTopic(), bookingRequest);

	}

	@Override
	public void saveCommunityHallBookingInit(CommunityHallBookingRequest bookingRequest) {

		// TODO: Bass user id in created by and last modified by
		CommunityHallBookingDetail bookingDetail = bookingRequest.getHallsBookingApplication();
		CommunityHallBookingRequestInit testPersist = CommunityHallBookingRequestInit.builder()
				.bookingId(bookingDetail.getBookingId()).tenantId(bookingDetail.getTenantId())
				.communityHallId(bookingDetail.getCommunityHallId()).bookingStatus(bookingDetail.getBookingStatus())
				.bookingDetails(bookingRequest.getHallsBookingApplication()).createdBy("12233")
				.createdDate(CommunityHallBookingUtil.getCurrentDateTime()).lastModifiedBy("1223")
				.lastModifiedDate(CommunityHallBookingUtil.getCurrentDateTime()).build();
		CommunityHallBokingInitDetails bookingPersiter = CommunityHallBokingInitDetails.builder()
				.hallsBookingApplication(testPersist).build();
		producer.push(bookingConfiguration.getCommunityHallBookingInitSaveTopic(), bookingPersiter);

	}

	@Override
	public List<CommunityHallBookingDetail> getBookingDetails(
			CommunityHallBookingSearchCriteria bookingSearchCriteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getCommunityHallBookingSearchQuery(bookingSearchCriteria, preparedStmtList);

		log.info("getBookingDetails : Final query: " + query);
		List<CommunityHallBookingDetail> bookingDetails = jdbcTemplate.query(query, preparedStmtList.toArray(),
				bookingRowmapper);
		
		HashMap<String , CommunityHallBookingDetail> bookingMap =  bookingDetails.stream().collect(Collectors.toMap(CommunityHallBookingDetail::getBookingId,
	           Function.identity(),
	            (left, right) -> left,
	            HashMap::new));
		
		List<String> bookingIds = new ArrayList<String>();
		bookingIds.addAll(bookingMap.keySet());
		
	//	List<String> bookingIdPreparestStatementList = new ArrayList<>();
		List<BankDetails> bankDetails = jdbcTemplate.query(queryBuilder.getBankDetailsQuery(bookingIds), bookingIds.toArray(), bankDetailsRowMapper);
		bankDetails.stream().forEach(bankDetail -> {
			bookingMap.get(bankDetail.getBookingId()).setBankDetails(bankDetail);
		});
		
		List<DocumentDetails> documentDetails = jdbcTemplate.query(queryBuilder.getDocumentDetailsQuery(bookingIds), bookingIds.toArray(),
				detailsRowMapper);
		
		documentDetails.stream().forEach(documentDetail -> {
			bookingMap.get(documentDetail.getBookingId()).addUploadedDocumentDetailsItem(documentDetail);
		});
		
		return bookingDetails;
	}

}
