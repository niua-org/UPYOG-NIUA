package org.upyog.chb.repository.querybuilder;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.web.models.VenueBookingSearchCriteria;
import org.upyog.chb.web.models.VenueSlotSearchCriteria;

/**
 * This class is responsible for building SQL queries for the Community Hall Booking module.
 * 
 * Purpose:
 * - To centralize and manage all SQL query construction logic for the module.
 * - To dynamically build queries based on search criteria and other parameters.
 * 
 * Dependencies:
 * - CommunityHallBookingConfiguration: Provides configuration properties for query construction.
 * - CommunityHallBookingSearchCriteria: Contains search parameters for booking-related queries.
 * - CommunityHallSlotSearchCriteria: Contains search parameters for slot-related queries.
 * 
 * Features:
 * - Provides static query templates for booking details, slot details, and document details.
 * - Dynamically appends conditions to the base queries based on the provided search criteria.
 * - Ensures that queries are optimized and secure by using parameterized inputs.
 * 
 * Query Templates:
 * 1. bookingDetailsQuery:
 *    - Retrieves booking details along with applicant and address information.
 *    - Joins multiple tables to fetch comprehensive booking data.
 * 
 * 2. slotDetailsQuery:
 *    - Retrieves slot details for the specified booking IDs.
 * 
 * 3. documentDetailsQuery:
 *    - Retrieves document details for the specified booking IDs.
 * 
 * Usage:
 * - This class is used by the repository layer to execute database operations.
 * - It ensures consistency and reusability of query logic across the application.
 */
/**
 * 
 */
@Component
public class CommunityHallBookingQueryBuilder {

	@Autowired
	private CommunityHallBookingConfiguration bookingConfiguration;

	private static final StringBuilder bookingDetailsQuery = new StringBuilder(
			"SELECT ecbd.booking_id, booking_no, payment_date, application_date, tenant_id, venue_code, \n"
					+ "booking_status, special_category, purpose, purpose_description, receipt_no, ecbd.createdby, ecbd.createdtime, \n"
					+ "ecbd.lastmodifiedby, ecbd.lastmodifiedtime,ecbd.permission_letter_filestore_id, ecbd.payment_receipt_filestore_id, \n" 
					+ "appl.applicant_detail_id, applicant_name, applicant_email_id, applicant_mobile_no,\n"
					+ "applicant_alternate_mobile_no, account_no, ifsc_code, bank_name, bank_branch_name, \n"
					+ "account_holder_name, address_id, door_no, house_no, address_line_1, \n"
					+ "landmark, city, city_code, pincode, street_name, locality, locality_code \n" 
					+ "FROM public.eg_chb_booking_detail ecbd \n"
					+ "join public.eg_chb_applicant_detail appl on ecbd.booking_id = appl.booking_id \n"
					+ "join public.eg_chb_address_detail addr on appl.applicant_detail_id = addr.applicant_detail_id ");

	private static final String slotDetailsQuery = "select * from public.eg_chb_slot_detail where booking_id in (";

	private static final String documentDetailsQuery = "select * from public.eg_chb_document_detail  where booking_id in (";

	private final String paginationWrapper = "SELECT * FROM " + "(SELECT *, DENSE_RANK() OVER (ORDER BY application_date DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private static final String COMMUNITY_HALL_SLOTS_AVAIALABILITY_QUERY =
			"SELECT ecbd.tenant_id, ecbd.venue_code, ecsd.capacity, ecsd.unit_code, ecsd.status, ecsd.booking_date, ecsd.booking_from_time, ecsd.booking_to_time \n"
			+ "\tFROM eg_chb_booking_detail ecbd\n"
			+ "\tjoin eg_chb_slot_detail ecsd on ecbd.booking_id = ecsd.booking_id\n"
			+ "\tLEFT JOIN eg_chb_payment_timer ecpt ON ecbd.booking_id = ecpt.booking_id\n"
			+ " where  ecbd.tenant_id= ? and ecbd.venue_code = ?\n"
			+ " and ecsd.status in ('BOOKED', 'PENDING_FOR_PAYMENT','BOOKING_CREATED') and \n"
			+ "\tecsd.booking_date >= CAST(? AS DATE) and ecsd.booking_date <= CAST(? AS DATE) ";
		//	+ "	AND ecsd.code in (?)";
	
	//private static final String COUNT_WRAPPER = " SELECT COUNT(*) FROM ({INTERNAL_QUERY}) AS count ";
	
	private static final String bookingDetailsCountCount = "SELECT count(ecbd.booking_id) \n" 
			+ "FROM public.eg_chb_booking_detail ecbd \n"
	+ "join public.eg_chb_applicant_detail appl on ecbd.booking_id = appl.booking_id \n";
	
	
	//public static final String PAYMENT_TIMER_INSERT_QUERY = "INSERT INTO eg_chb_payment_timer(booking_id, createdby, createdtime, status) VALUES (?, ?, ?, ?);";
	public static final String PAYMENT_TIMER_INSERT_QUERY = "INSERT INTO eg_chb_payment_timer(booking_id, createdby, createdtime, status, booking_no, venue_code, unit_code, booking_date, tenant_id, lastmodifiedby, lastmodifiedtime,start_time , end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? , ? ,?);";

	public static final String PAYMENT_TIMER_DELETE_FOR_BOOKING_ID_QUERY = "DELETE FROM eg_chb_payment_timer WHERE booking_id IN (%s)";
	
	public static final String PAYMENT_TIMER_SELECT_EXPIRED_QUERY = "SELECT * FROM eg_chb_payment_timer WHERE ? - createdtime > ? and status = ?";
	
	public static final String UPDATE_BOOKING_DETAIL_QUERY = "update eg_chb_booking_detail set booking_status = ?, lastmodifiedby = ?, lastmodifiedtime = ? "
			+ ", receipt_no = ?, payment_date = ? where booking_id in (?) ";
	
	public static final String UPDATE_BOOKING_SLOT_QUERY = "update eg_chb_slot_detail set status = ?, lastmodifiedby = ?, lastmodifiedtime = ? "
			+ " where booking_id in (?)  ";
	
	public static final String INSERT_BOOKING_DETAIL_AUDIT_QUERY = "INSERT INTO public.eg_chb_booking_detail_audit SELECT * FROM public.eg_chb_booking_detail WHERE booking_id in (?) ";
	
	public static final String INSERT_SLOT_DETAIL_AUDIT_QUERY = "INSERT INTO public.eg_chb_slot_detail_audit SELECT * FROM public.eg_chb_slot_detail WHERE booking_id in (?) ";
	
	public static final String GET_BOOKING_PAYMENT_TIMER_VALUE_QUERY = "SELECT * from eg_chb_payment_timer where  booking_id in (?)";
	
	
	public static final String UPADTE_BOOKING_PAYMENT_TIMER_VALUE_QUERY = "UPDATE eg_chb_payment_timer " +
            " SET status = ? WHERE booking_id = ?";

	public static final String UPDATE_TIMER_BOOKING_ID_QUERY = "UPDATE eg_chb_payment_timer "
			+ " SET booking_id = ?, booking_no = ? WHERE booking_id = ?";
	
	public static final String UPDATE_BOOKING_STATUS =  "update eg_chb_booking_detail set booking_status = ?, lastmodifiedby = ?, lastmodifiedtime = ? "
			+ " where booking_id in (?) ";
	
	public static final String SELECT_TIMER_QUERY = " SELECT * "
			+ "		    FROM eg_chb_payment_timer "
			+ "		    WHERE tenant_id = ? "
			+ "		      AND venue_code = ? "
			+ "		      AND unit_code = ? " 
			+"            AND start_time = ? AND end_time = ? "
			+ "		      AND booking_date BETWEEN ? AND ?;";
	
	
	/**
	 * To give the Search query based on the requirements.
	 * 
	 * @param criteria         Community Hall booking search criteria
	 * @param preparedStmtList values to be replaced on the query
	 * @return Final Search Query
	 */
	public String getCommunityHallBookingSearchQuery(VenueBookingSearchCriteria criteria,
			List<Object> preparedStmtList) {
		StringBuilder builder;
		
		if(criteria.isCountCall()) {
			builder = new StringBuilder(bookingDetailsCountCount);
		}else {
			builder = new StringBuilder(bookingDetailsQuery);
		}
		
		if(criteria.getFromDate() != null || criteria.getToDate() != null) {
			builder.append(" join public.eg_chb_slot_detail ecsd ON ecsd.booking_id = ecbd.booking_id ");
		}
		
		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ecbd.tenant_id like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ecbd.tenant_id=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getBookingIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecbd.booking_id IN (").append(createQueryParams(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String bookingNo = criteria.getBookingNo();
		if (bookingNo != null) {
			List<String> applicationNos = Arrays.asList(bookingNo.split(","));
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecbd.booking_no IN (").append(createQueryParams(applicationNos)).append(")");
			addToPreparedStatement(preparedStmtList, applicationNos);
		}
		
		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecbd.booking_status =  ? ");
			preparedStmtList.add(status);
		}
		
		if (criteria.getCommunityHallCode() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecbd.venue_code =  ? ");
			preparedStmtList.add(criteria.getCommunityHallCode());
		}
		
		String mobileNo = criteria.getMobileNumber();
		if (mobileNo != null) {
			List<String> mobileNos = Arrays.asList(mobileNo.split(","));
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" appl.applicant_mobile_no IN (").append(createQueryParams(mobileNos)).append(")");
			addToPreparedStatement(preparedStmtList, mobileNos);
		}

		//createdby search criteria
		List<String> createdBy = criteria.getCreatedBy();
		if (!CollectionUtils.isEmpty(createdBy)) {

			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecbd.createdby IN (").append(createQueryParams(createdBy)).append(")");
			addToPreparedStatement(preparedStmtList, createdBy);
		}

		//From booking date to booking date search criteria
		final String DATE_CAST = " ?::DATE ";
		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecsd.booking_date BETWEEN ").append(DATE_CAST).append(" AND ")
					.append(DATE_CAST);
			preparedStmtList.add(criteria.getFromDate());
			preparedStmtList.add(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecsd.booking_date >= ").append(DATE_CAST);
			preparedStmtList.add(criteria.getFromDate());
		} else if (criteria.getFromDate() == null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ecsd.booking_date <= ").append(DATE_CAST);
			preparedStmtList.add(criteria.getToDate());
		}
		
		String query = null;
		
		if(criteria.isCountCall()) {
			//pagination attributes not required for count query
			query = builder.toString();
		} else {
			//Add pagination attributes for booking details query
			query = addPaginationWrapper(builder.toString(), preparedStmtList, criteria);
		}
		
		return query;
	}
	

	/**
	 * add if clause to the Statement if required or else AND
	 * 
	 * @param values
	 * @param queryString
	 */
	private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
		if (values.isEmpty())
			queryString.append(" WHERE ");
		else {
			queryString.append(" AND");
		}
	}

	/**
	 * add values to the preparedStatment List
	 * 
	 * @param preparedStmtList
	 * @param ids
	 */
	private void addToPreparedStatement(List<Object> preparedStmtList, List<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});
	}

	/**
	 * produce a query input for the multiple values
	 * 
	 * @param ids
	 * @return
	 */
	private Object createQueryParams(List<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param query            prepared Query
	 * @param preparedStmtList values to be replased on the query
	 * @param criteria         bpa search criteria
	 * @return the query by replacing the placeholders with preparedStmtList
	 */
	private String addPaginationWrapper(String query, List<Object> preparedStmtList,
			VenueBookingSearchCriteria criteria) {

		int limit = bookingConfiguration.getDefaultLimit();
		int offset = bookingConfiguration.getDefaultOffset();
		String finalQuery = paginationWrapper.replace("{}", query);

		if (criteria.getLimit() == null && criteria.getOffset() == null) {
			limit = bookingConfiguration.getMaxSearchLimit();
		}

		if (criteria.getLimit() != null && criteria.getLimit() <= bookingConfiguration.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > bookingConfiguration.getMaxSearchLimit()) {
			limit = bookingConfiguration.getMaxSearchLimit();
		}

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		if (limit == -1) {
			finalQuery = finalQuery.replace("WHERE offset_ > ? AND offset_ <= ?", "");
		} else {
			preparedStmtList.add(offset);
			preparedStmtList.add(limit + offset);
		}

		return finalQuery;

	}

	/*
	 * @SuppressWarnings({"rawtypes" }) private StringBuilder addPagingClause(final
	 * StringBuilder selectQuery, final List preparedStatementValues, final
	 * BillSearchCriteria searchBillCriteria) {
	 * 
	 * StringBuilder finalQuery;
	 * 
	 * if (searchBillCriteria.getRetrieveOldest()) finalQuery = new
	 * StringBuilder(BILL_MIN_QUERY.replace(REPLACE_STRING, selectQuery)); else
	 * finalQuery = new StringBuilder(BILL_MAX_QUERY.replace(REPLACE_STRING,
	 * selectQuery));
	 * 
	 * if (searchBillCriteria.isOrderBy()) {
	 * finalQuery.append(" ORDER BY billresult.bd_consumercode "); }
	 * 
	 * return finalQuery; }
	 */

	/**
	 * Builds a query to fetch slot rows for the provided booking ids.
	 *
	 * @param bookingIds booking ids to include in the query
	 * @return SQL query string with parameter placeholders for the booking ids
	 */
	public String getSlotDetailsQuery(List<String> bookingIds) {
		StringBuilder builder = new StringBuilder(slotDetailsQuery);
		builder.append(createQueryParams(bookingIds)).append(")");
		return builder.toString();

	}

	/**
	 * Builds a query to fetch document rows for the provided booking ids.
	 *
	 * @param bookingIds booking ids to include in the query
	 * @return SQL query string with parameter placeholders for the booking ids
	 */
	public String getDocumentDetailsQuery(List<String> bookingIds) {
		StringBuilder builder = new StringBuilder(documentDetailsQuery);
		builder.append(createQueryParams(bookingIds)).append(")");
		return builder.toString();
	}




	/**
	 * Builds the query used to fetch community hall slot availability.
	 *
	 * <p>
	 * The query uses placeholders for tenant/venue and booking window, and it also
	 * appends optional time-range filters based on {@link VenueSlotSearchCriteria}.
	 * </p>
	 *
	 * @param searchCriteria slot availability search criteria
	 * @param paramsList list to be populated with placeholder parameters (mutated)
	 * @return SQL query string builder
	 */
	public StringBuilder getCommunityHallSlotAvailabilityQuery(VenueSlotSearchCriteria searchCriteria,
			List<Object> paramsList) {

		StringBuilder builder = new StringBuilder(COMMUNITY_HALL_SLOTS_AVAIALABILITY_QUERY);

		paramsList.add(searchCriteria.getTenantId());
		paramsList.add(searchCriteria.getVenueCode());
//		paramsList.add(SlotStatusEnum.BOOKED.toString());
		paramsList.add(searchCriteria.getBookingStartDate());
		paramsList.add(searchCriteria.getBookingEndDate());
		appendTimeFilters(searchCriteria, builder, paramsList);

		return builder;
	}


	
	/**
	 * Appends time-based filtering conditions to the dynamic SQL query based on the
	 * provided search criteria.
	 *
	 * <p>
	 * This method applies proper time range overlap logic to ensure correct booking
	 * slot filtering. It supports filtering based on:
	 * <ul>
	 * <li>Both fromTime and toTime (overlap condition)</li>
	 * <li>Only fromTime (end-time based filtering)</li>
	 * <li>Only toTime (start-time based filtering)</li>
	 * </ul>
	 *
	 * <p>
	 * When both times are provided, the method uses interval overlap logic:
	 * 
	 * <pre>
	 * existing_from_time < requested_to_time
	 * AND existing_to_time > requested_from_time
	 * </pre>
	 *
	 * @param searchCriteria contains fromTime and/or toTime filter values
	 * @param builder        StringBuilder used to construct the SQL query
	 *                       dynamically
	 * @param paramsList     list of query parameters to be bound in
	 *                       PreparedStatement
	 */
	private void appendTimeFilters(VenueSlotSearchCriteria searchCriteria, StringBuilder builder,
			List<Object> paramsList) {
		if (StringUtils.isNotBlank(searchCriteria.getFromTime())
				&& StringUtils.isNotBlank(searchCriteria.getToTime())) {

			builder.append(
					" AND ecsd.booking_to_time >= CAST(? AS TIME) "
							+ " AND ecsd.booking_from_time <= CAST(? AS TIME) ");

			// Placeholder order must match the query above
			paramsList.add(searchCriteria.getToTime());
			paramsList.add(searchCriteria.getFromTime());

		} else if (StringUtils.isNotBlank(searchCriteria.getFromTime())) {

			builder.append(" AND ecsd.booking_to_time > CAST(? AS TIME) ");
			paramsList.add(searchCriteria.getFromTime());

		} else if (StringUtils.isNotBlank(searchCriteria.getToTime())) {

			builder.append(" AND ecsd.booking_from_time < CAST(? AS TIME) ");
			paramsList.add(searchCriteria.getToTime());
		}

	}

}
