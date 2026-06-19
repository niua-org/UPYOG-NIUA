package org.upyog.chb.service.impl;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.upyog.chb.constants.CommunityHallBookingConstants;
import org.upyog.chb.enums.BookingStatusEnum;
import org.upyog.chb.repository.CommunityHallBookingRepository;
import org.upyog.chb.service.BookingTimerService;
import org.upyog.chb.service.CHBEncryptionService;
import org.upyog.chb.service.CommunityHallBookingService;
import org.upyog.chb.service.DemandService;
import org.upyog.chb.service.EnrichmentService;
import org.upyog.chb.service.WorkflowService;
import org.upyog.chb.util.CommunityHallBookingUtil;
import org.upyog.chb.util.MdmsUtil;
import org.upyog.chb.validator.CommunityHallBookingValidator;
import org.upyog.chb.web.models.ApplicantDetail;
import org.upyog.chb.web.models.BookingPaymentTimerDetails;
import org.upyog.chb.web.models.VenueBookingDetail;
import org.upyog.chb.web.models.VenueBookingRequest;
import org.upyog.chb.web.models.VenueBookingSearchCriteria;
import org.upyog.chb.web.models.VenueSlotAvailabilityDetail;
import org.upyog.chb.web.models.VenueSlotAvailabilityResponse;
import org.upyog.chb.web.models.VenueSlotSearchCriteria;
import org.upyog.chb.web.models.workflow.State;

import digit.models.coremodels.PaymentDetail;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This class implements the CommunityHallBookingService interface and provides
 * the business logic for the Community Hall Booking module.
 * 
 * Purpose:
 * - To handle all service-level operations related to community hall bookings, such as
 *   creating, updating, validating, and retrieving booking records.
 * - To coordinate between the repository, validation, workflow, and enrichment layers.
 * 
 * Dependencies:
 * - CommunityHallBookingRepository: Handles database operations for bookings.
 * - CommunityHallBookingValidator: Validates booking requests and search criteria.
 * - WorkflowService: Manages workflow-related operations for bookings.
 * - EnrichmentService: Enriches booking requests with additional data.
 * - DemandService: Handles demand generation and payment-related operations.
 * - MdmsUtil: Fetches and processes master data from MDMS.
 * - CHBEncryptionService: Handles encryption and decryption of sensitive booking data.
 * 
 * Features:
 * - Provides methods to create and update bookings while ensuring data validation and enrichment.
 * - Integrates with the workflow service to manage booking states.
 * - Handles slot availability checks and demand generation for bookings.
 * - Logs important operations and errors for debugging and monitoring purposes.
 * 
 * Usage:
 * - This class is automatically managed by Spring and injected wherever the
 *   CommunityHallBookingService interface is required.
 * - It ensures consistent and reusable business logic for the Community Hall Booking module.
 */
@Service
@Slf4j
public class CommunityHallBookingServiceImpl implements CommunityHallBookingService {

	@Autowired
	private CommunityHallBookingRepository bookingRepository;
	@Autowired
	private CommunityHallBookingValidator hallBookingValidator;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private DemandService demandService;

	@Autowired
	private MdmsUtil mdmsUtil;
	
	@Autowired
	private CHBEncryptionService encryptionService;
	
	@Autowired
	private BookingTimerService bookingTimerService;
	
	@Override
	public VenueBookingDetail createBooking(@Valid VenueBookingRequest venueBookingRequest) {
		log.info("Create community hall booking for user : "
				+ venueBookingRequest.getRequestInfo().getUserInfo().getUuid());
		// TODO move to util calls and validate tenant id in the controller layer
		String tenantId = venueBookingRequest.getHallsBookingApplication().getTenantId().split("\\.")[0];
		if (venueBookingRequest.getHallsBookingApplication().getTenantId().split("\\.").length == 1) {
			throw new CustomException(CommunityHallBookingConstants.INVALID_TENANT,
					"Please provide valid tenant id for booking creation");
		}

		Object mdmsData = mdmsUtil.mDMSCall(venueBookingRequest.getRequestInfo(), venueBookingRequest.getHallsBookingApplication().getTenantId());

		// 1. Validate request master data to confirm it has only valid data in records
		hallBookingValidator.validateCreate(venueBookingRequest, mdmsData);
		// 2. Add fields that has custom logic like booking no, ids using UUID
		enrichmentService.enrichCreateBookingRequest(venueBookingRequest);
		
		//ENcrypt PII data of applicant
		encryptionService.encryptObject(venueBookingRequest);

		/**
		 * Workflow will come into picture once hall location changes or booking is
		 * cancelled otherwise after payment booking will be auto approved
		 * 
		 */

		// 3.Update workflow of the application
		// workflowService.updateWorkflow(communityHallsBookingRequest,
		// WorkflowStatus.CREATE);

		demandService.createDemand(venueBookingRequest, mdmsData, true);

		// 4.Persist the request using persister service
		bookingRepository.saveCommunityHallBooking(venueBookingRequest);
		/*
		 * Slot-search can reserve hall slots before a booking id exists. In that case,
		 * the timer table stores draftId in booking_id. Once create generates the real
		 * booking id and booking number, the timer rows are moved to the final booking
		 * reference and the remaining timer value is returned in the create response.
		 */
		bookingRepository.updateTimerBookingId(
				venueBookingRequest.getHallsBookingApplication().getBookingId(),
				venueBookingRequest.getHallsBookingApplication().getBookingNo(),
				venueBookingRequest.getHallsBookingApplication().getDraftId());
		venueBookingRequest.getHallsBookingApplication().setTimerValue(bookingTimerService
				.getRemainingTimerValue(venueBookingRequest.getHallsBookingApplication().getBookingId()));

		return venueBookingRequest.getHallsBookingApplication();
	}
	
	@Override
	public VenueBookingDetail createInitBooking(
			@Valid VenueBookingRequest venueBookingRequest) {
		log.info("Create community hall temp booking for user : "
				+ venueBookingRequest.getRequestInfo().getUserInfo().getUuid());
		bookingRepository.saveCommunityHallBookingInit(venueBookingRequest);
		return null;
	}

	@Override
	public List<VenueBookingDetail> getBookingDetails(VenueBookingSearchCriteria bookingSearchCriteria,
			RequestInfo info) {
		hallBookingValidator.validateSearch(info, bookingSearchCriteria);
		List<VenueBookingDetail> bookingDetails = new ArrayList<VenueBookingDetail>();
		bookingSearchCriteria  = addCreatedByMeToCriteria(bookingSearchCriteria, info);
		
		log.info("loading data based on criteria" + bookingSearchCriteria);
		
		if (bookingSearchCriteria.getMobileNumber() != null
				&& bookingSearchCriteria.getMobileNumber().trim().length() > 9) {

			ApplicantDetail applicantDetail = ApplicantDetail.builder()
					.applicantMobileNo(bookingSearchCriteria.getMobileNumber()).build();
			VenueBookingDetail communityHallBookingDetail = VenueBookingDetail.builder()
					.applicantDetail(applicantDetail).build();
			VenueBookingRequest bookingRequest = VenueBookingRequest.builder()
					.hallsBookingApplication(communityHallBookingDetail).requestInfo(info).build();

			communityHallBookingDetail = encryptionService.encryptObject(bookingRequest);

			bookingSearchCriteria
					.setMobileNumber(communityHallBookingDetail.getApplicantDetail().getApplicantMobileNo());

			log.info("loading data based on criteria after encrypting mobile no : " + bookingSearchCriteria);

		}
		
		bookingDetails = bookingRepository.getBookingDetails(bookingSearchCriteria);
		if(CollectionUtils.isEmpty(bookingDetails)) {
			return bookingDetails;
		}
		bookingDetails = encryptionService.decryptObject(bookingDetails, info);
		
		return bookingDetails;
	}
	
	
	@Override
	public Integer getBookingCount(@Valid VenueBookingSearchCriteria criteria,
			@NonNull RequestInfo requestInfo) {
		criteria.setCountCall(true);
		Integer bookingCount = 0;
		
		criteria  = addCreatedByMeToCriteria(criteria, requestInfo);
		bookingCount = bookingRepository.getBookingCount(criteria);
		
		return bookingCount;
	}

	
	private VenueBookingSearchCriteria addCreatedByMeToCriteria(VenueBookingSearchCriteria criteria, RequestInfo requestInfo) {
		if(requestInfo.getUserInfo() == null) {
			log.info("Request info is null returning criteira");
			return criteria;
		}
		List<String> roles = new ArrayList<>();
		for (Role role : requestInfo.getUserInfo().getRoles()) {
			roles.add(role.getCode());
		}
		log.info("user roles for searching : " + roles);
		/**
		 * Citizen can see booking details only booked by him
		 */
		List<String> uuids = new ArrayList<>();
		if (roles.contains(CommunityHallBookingConstants.CITIZEN) && !StringUtils.isEmpty(requestInfo.getUserInfo().getUuid())) {
			uuids.add(requestInfo.getUserInfo().getUuid());
			criteria.setCreatedBy(uuids);
			log.debug("loading data of created and by me" + uuids.toString());
		}
		return criteria;
	}

	@Override
	public VenueBookingDetail updateBooking(VenueBookingRequest venueBookingRequest,
			PaymentDetail paymentDetail, BookingStatusEnum status) {
		String bookingNo = venueBookingRequest.getHallsBookingApplication().getBookingNo();
		log.info("Updating booking for booking no : " + bookingNo);
		if (bookingNo == null) {
			throw new CustomException("INVALID_BOOKING_CODE",
					"Booking no not valid. Failed to update booking status for : " + bookingNo);
		}
		VenueBookingSearchCriteria bookingSearchCriteria = VenueBookingSearchCriteria.builder()
				.bookingNo(bookingNo).build();
		List<VenueBookingDetail> bookingDetails = bookingRepository.getBookingDetails(bookingSearchCriteria);
		if (bookingDetails.size() == 0) {
			throw new CustomException("INVALID_BOOKING_CODE",
					"Booking no not valid. Failed to update booking status for : " + bookingNo);
		}
		
		hallBookingValidator.validateUpdate(venueBookingRequest.getHallsBookingApplication(), bookingDetails.get(0));

		convertBookingRequest(venueBookingRequest, bookingDetails.get(0));

		
		//Update payment date and receipt no on successful payment when payment detail object is received
		if (paymentDetail != null) {
			venueBookingRequest.getHallsBookingApplication().setReceiptNo(paymentDetail.getReceiptNumber());
			venueBookingRequest.getHallsBookingApplication().setPaymentDate(paymentDetail.getReceiptDate());
		}
		//Update workflow of booking application for refund when the workflow object is not null in payload
		if (venueBookingRequest.getHallsBookingApplication().getWorkflow()!=null) {
			State state = workflowService.updateWorkflow(venueBookingRequest);
			status = BookingStatusEnum.valueOf(state.getApplicationStatus());
		}
		enrichmentService.enrichUpdateBookingRequest(venueBookingRequest, status);
		bookingRepository.updateBooking(venueBookingRequest);
		log.info("fetched booking detail and updated status "
				+ venueBookingRequest.getHallsBookingApplication().getBookingStatus());
		return venueBookingRequest.getHallsBookingApplication();
	}
	
	/**
	 * We are updating booking status synchronously for updating booking status on payment success 
	 * Deleting the timer entry here after successful update of booking
	 */
	@Transactional
	@Override
	public void updateBookingSynchronously(VenueBookingRequest venueBookingRequest,
			PaymentDetail paymentDetail, BookingStatusEnum status, boolean deleteBookingTimer) {
		String bookingNo = venueBookingRequest.getHallsBookingApplication().getBookingNo();
		log.info("Updating booking synchronously for booking no : " + bookingNo);
		if (bookingNo == null) {
			throw new CustomException("INVALID_BOOKING_CODE",
					"Booking no not valid. Failed to update booking status for : " + bookingNo);
		}
		VenueBookingSearchCriteria bookingSearchCriteria = VenueBookingSearchCriteria.builder()
				.bookingNo(bookingNo).build();
		List<VenueBookingDetail> bookingDetails = bookingRepository.getBookingDetails(bookingSearchCriteria);
		if (bookingDetails.size() == 0) {
			throw new CustomException("INVALID_BOOKING_CODE",
					"Booking no not valid. Failed to update booking status for : " + bookingNo);
		}
		VenueBookingDetail bookingDetail = bookingDetails.get(0);
		venueBookingRequest.setHallsBookingApplication(bookingDetail);
		bookingRepository.updateBookingSynchronously(bookingDetail.getBookingId(), venueBookingRequest.getRequestInfo().getUserInfo().getUuid(), paymentDetail, status.toString());
		if(deleteBookingTimer) {
			log.info("Deleting booking timer with booking id  {}", venueBookingRequest.getHallsBookingApplication().getBookingId());
			bookingTimerService.deleteBookingTimer(venueBookingRequest.getHallsBookingApplication().getBookingId(), false);
		}
	}

	private void convertBookingRequest(VenueBookingRequest venueBookingRequest,
			VenueBookingDetail bookingDetailDB) {
		VenueBookingDetail bookingDetailRequest = venueBookingRequest.getHallsBookingApplication();
		if (bookingDetailDB.getPermissionLetterFilestoreId() == null
				&& bookingDetailRequest.getPermissionLetterFilestoreId() != null) {
			bookingDetailDB.setPermissionLetterFilestoreId(bookingDetailRequest.getPermissionLetterFilestoreId());
		}
 
		if (bookingDetailDB.getPaymentReceiptFilestoreId() == null
				&& bookingDetailRequest.getPaymentReceiptFilestoreId() != null) {
			bookingDetailDB.setPaymentReceiptFilestoreId(bookingDetailRequest.getPaymentReceiptFilestoreId());
		}
		if(bookingDetailRequest.getWorkflow()!=null) {
			bookingDetailDB.setWorkflow(bookingDetailRequest.getWorkflow());
		}
		venueBookingRequest.setHallsBookingApplication(bookingDetailDB);
		
	}

	/**
	 * Retrieves community hall slot availability for the given search criteria.
	 *
	 * <p>
	 * This service method resolves slot availability, applies existing timer row
	 * state, and optionally creates or reuses a payment timer when the client
	 * requests a timer hold.
	 * </p>
	 *
	 * @param criteria slot search criteria containing hall codes, booking dates, and timer flags
	 * @param info     request metadata and authenticated user details
	 * @return availability response with slots, status, draft id, and timer value
	 */
	@Override
	public VenueSlotAvailabilityResponse getCommunityHallSlotAvailability(
			VenueSlotSearchCriteria criteria, RequestInfo info) {
		if (criteria.getVenueCode() == null && CollectionUtils.isEmpty(criteria.getCodes())) {
			throw new CustomException("INVALID_HALL_CODE", "Invalid hall code provided for slot search");
		}
		log.info("criteria : {}", criteria);
		List<VenueSlotAvailabilityDetail> availabiltityDetails = bookingRepository
				.getCommunityHallSlotAvailability(criteria);
		log.info("Availabiltity details fetched from DB :" + availabiltityDetails);

		List<VenueSlotAvailabilityDetail> availabiltityDetailsList = convertToCommunityHallAvailabilityResponse(
				criteria, availabiltityDetails);

		Long timerValue = -1l;
		availabiltityDetailsList = checkTimerTableForAvailaibility(info, criteria, availabiltityDetailsList);
		boolean bookingAllowed = availabiltityDetailsList.stream()
				.anyMatch(detail -> BookingStatusEnum.BOOKED.toString().equals(detail.getSlotStaus()));

		if (!bookingAllowed && Boolean.TRUE.equals(criteria.getIsTimerRequired())) {
			timerValue = bookingTimerService.managePaymentTimer(criteria, info, availabiltityDetailsList);
		}

		VenueSlotAvailabilityResponse hallSlotAvailabilityResponse = VenueSlotAvailabilityResponse
				.builder().hallSlotAvailabiltityDetails(availabiltityDetailsList).timerValue(timerValue)
				.draftId(criteria.getDraftId()).build();

		log.info("Availability details response after updating status :" + hallSlotAvailabilityResponse);
		return hallSlotAvailabilityResponse;
	}



private List<VenueSlotAvailabilityDetail> checkTimerTableForAvailaibility(
            RequestInfo info, VenueSlotSearchCriteria criteria,
		List<VenueSlotAvailabilityDetail> availabilityDetails) {

	List<BookingPaymentTimerDetails> timerDetails = bookingTimerService.getBookingFromTimerTable(info, criteria);

	// If timer details are null or empty, return availability details as is
	if (timerDetails == null || timerDetails.isEmpty()) {
		log.info("Timer details are null or empty, returning availability details as is.");
		return availabilityDetails;
	}

	Map<VenueSlotAvailabilityDetail, VenueSlotAvailabilityDetail> slotDetailsMap = availabilityDetails
			.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
	log.info("Timer Details from db : " + timerDetails);

		timerDetails.forEach(detail -> applyTimerDetailToAvailability(info, criteria, availabilityDetails, slotDetailsMap,
				detail));

	return availabilityDetails;
}

	private void applyTimerDetailToAvailability(RequestInfo info, VenueSlotSearchCriteria criteria,
			List<VenueSlotAvailabilityDetail> availabilityDetails,
			Map<VenueSlotAvailabilityDetail, VenueSlotAvailabilityDetail> slotDetailsMap,
			BookingPaymentTimerDetails detail) {
		VenueSlotAvailabilityDetail availabilityDetail = VenueSlotAvailabilityDetail.builder()
				.venueCode(detail.getVenuecode()).code(detail.getCode())
				.bookingDate(CommunityHallBookingUtil.parseLocalDateToString(detail.getBookingDate(),
						CommunityHallBookingConstants.DATE_FORMAT))
				.tenantId(detail.getTenantId()).build();

		if (availabilityDetails.contains(availabilityDetail)) {
			log.info("Booking created by user id {} and booking id {} ", criteria.getBookingId(),
					info.getUserInfo().getUuid());
			VenueSlotAvailabilityDetail slotAvailabilityDetail = slotDetailsMap.get(availabilityDetail);
			log.info("Slot Availability detail ::: " + slotAvailabilityDetail.toString());
			boolean isCreatedByCurrentUser = detail.getCreatedBy().equals(info.getUserInfo().getUuid());
			boolean existingBookingIdCheck = detail.getBookingId().equals(getTimerBookingReference(criteria));

			String fromTime = criteria.getFromTime() != null ? criteria.getFromTime() : (criteria.getStartTime() != null ? criteria.getStartTime().toString() : null);
			String toTime = criteria.getToTime() != null ? criteria.getToTime() : (criteria.getEndTime() != null ? criteria.getEndTime().toString() : null);
			slotAvailabilityDetail.setFromTime(fromTime);
			slotAvailabilityDetail.setToTime(toTime);
			if (isCreatedByCurrentUser && existingBookingIdCheck) {
				log.info("inside booking created by me with same booking id ");
				slotAvailabilityDetail.setSlotStaus(BookingStatusEnum.AVAILABLE.toString());
			} else {
				slotAvailabilityDetail.setSlotStaus(BookingStatusEnum.BOOKED.toString());
			}
		}
	}

	private String getTimerBookingReference(VenueSlotSearchCriteria criteria) {
		return StringUtils.isNotBlank(criteria.getBookingId()) ? criteria.getBookingId() : criteria.getDraftId();
	}



	/**
	 * 
	 * @param criteria
	 * @param availabiltityDetails
	 * @return
	 */
	private List<VenueSlotAvailabilityDetail> convertToCommunityHallAvailabilityResponse(
			VenueSlotSearchCriteria criteria, List<VenueSlotAvailabilityDetail> availabiltityDetails) {

		List<VenueSlotAvailabilityDetail> availabiltityDetailsList = new ArrayList<VenueSlotAvailabilityDetail>();
		LocalDate startDate = CommunityHallBookingUtil.parseStringToLocalDate(criteria.getBookingStartDate());

		LocalDate endDate = CommunityHallBookingUtil.parseStringToLocalDate(criteria.getBookingEndDate());

		List<LocalDate> totalDates = new ArrayList<>();
		//Calculating list of dates for booking
		while (!startDate.isAfter(endDate)) {
			totalDates.add(startDate);
			startDate = startDate.plusDays(1);
		}
		
		//Move the no of days to application properties File
		if(totalDates.size() > 3) {
			throw new CustomException(CommunityHallBookingConstants.INVALID_BOOKING_DATE_RANGE,
					"Booking is not allowed for this no of days.");
		}

		Map<String, VenueSlotAvailabilityDetail> dbAvailabilityDetails = availabiltityDetails.stream()
				.collect(Collectors.toMap(detail -> buildSlotKey(detail.getTenantId(), detail.getVenueCode(),
						detail.getCode(), detail.getBookingDate()), Function.identity(), (first, second) -> first));
		for (LocalDate date : totalDates) {
			List<String> hallCodes = new ArrayList<>();
			if (StringUtils.isNotBlank(criteria.getCode())) {
				hallCodes.add(criteria.getCode());
			} else {
				hallCodes.addAll(criteria.getCodes());
			}
			for (String data : hallCodes) {
				String bookingDate = CommunityHallBookingUtil.parseLocalDateToString(date,
					CommunityHallBookingConstants.DATE_FORMAT);
				String key = buildSlotKey(criteria.getTenantId(), criteria.getVenueCode(), data, bookingDate);
				VenueSlotAvailabilityDetail existingDetail = dbAvailabilityDetails.get(key);
				if (existingDetail != null) {
					availabiltityDetailsList.add(existingDetail);
				} else {
					availabiltityDetailsList.add(createCommunityHallSlotAvailabiltityDetail(criteria, date, data));
				}
			}
		}

		//Setting hall status to booked if it is already booked by checking in the database entry
		for (VenueSlotAvailabilityDetail detail : availabiltityDetailsList) {
			int index = availabiltityDetails.indexOf(detail);
			if (index >= 0) {
				VenueSlotAvailabilityDetail dbDetail = availabiltityDetails.get(index);
				detail.setSlotStaus(BookingStatusEnum.BOOKED.toString());
				detail.setFromTime(dbDetail.getFromTime());
				detail.setToTime(dbDetail.getToTime());
			}
		}
		
		return availabiltityDetailsList;
	}

	private String buildSlotKey(String tenantId, String venueCode, String code, String bookingDate) {
		return tenantId + "|" + venueCode + "|" + code + "|" + bookingDate;
	}

	private VenueSlotAvailabilityDetail createCommunityHallSlotAvailabiltityDetail(
			VenueSlotSearchCriteria criteria, LocalDate date, String hallCode) {
		VenueSlotAvailabilityDetail availabiltityDetail = VenueSlotAvailabilityDetail.builder()
				.venueCode(criteria.getVenueCode()).code(hallCode)
			//Setting slot status available for every hall and hall code
				.slotStaus(BookingStatusEnum.AVAILABLE.toString()).tenantId(criteria.getTenantId())
//				.fromTime(fromTime.toString()).toTime(toTime.toString())
				.bookingDate(CommunityHallBookingUtil.parseLocalDateToString(date, "dd-MM-yyyy")).build();
		return availabiltityDetail;
	}


}
