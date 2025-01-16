package org.upyog.chb.service;

import static org.upyog.chb.constants.CommunityHallBookingConstants.CHB_ACTION_MOVETOEMPLOYEE;
import static org.upyog.chb.constants.CommunityHallBookingConstants.CHB_REFUND_BUSINESSSERVICE;
import static org.upyog.chb.constants.CommunityHallBookingConstants.CHB_REFUND_MODULENAME;
import static org.upyog.chb.constants.CommunityHallBookingConstants.CHB_STATUS_BOOKED;
import static org.upyog.chb.constants.CommunityHallBookingConstants.CHB_TENANTID;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.upyog.chb.enums.BookingStatusEnum;
import org.upyog.chb.repository.CommunityHallBookingRepository;
import org.upyog.chb.util.CommunityHallBookingUtil;
import org.upyog.chb.web.models.BookingPaymentTimerDetails;
import org.upyog.chb.web.models.CommunityHallBookingDetail;
import org.upyog.chb.web.models.CommunityHallBookingRequest;
import org.upyog.chb.web.models.CommunityHallBookingSearchCriteria;
import org.upyog.chb.web.models.workflow.ProcessInstance;
import org.upyog.chb.web.models.workflow.State;

import digit.models.coremodels.UserDetailResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SchedulerService {

	@Autowired
	private CommunityHallBookingService communityHallBookingService;
	
	@Autowired
	private CommunityHallBookingRepository bookingRepository;

	@Autowired
	private UserService userService;
	
	@Autowired
	private WorkflowService workflowService;
	/*
	 * This scheduler runs every 5 mins to delete the bookingId from the
	 * paymentTimer table when the timer is expired or payment is failed
	 */
	@Scheduled(fixedRate = 5 * 60 * 1000) // Runs every 5 minutes
	public void cleanupExpiredEntries() {
		log.info("Delete Expired Booking task running...:::.....:::");
		deleteExpiredBookings();
	}

	@Transactional
	public void deleteExpiredBookings() {
		List<BookingPaymentTimerDetails> bookingPaymentTimerDetails = bookingRepository.getExpiredBookingTimer();
		List<String> bookingList = bookingPaymentTimerDetails.stream().map(detail -> detail.getBookingId())
				.collect(Collectors.toList());

		log.info("Expired booking id list  : {}", bookingList);

		String bookingIds = String.join(",", bookingList);

		bookingRepository.deleteBookingTimer(bookingIds, true);

	}

//	@Scheduled(fixedRate = 10 * 60 * 1000)
	@Scheduled(cron = "0 0 23 * * *")
	public void updateWorkflowForBookedApplications() {
		log.info("Scheduler - Updating Workflow of Booked applications...:::.....:::");
		String formattedDate = CommunityHallBookingUtil.parseLocalDateToString(LocalDate.now(), "yyyy-MM-dd");
		CommunityHallBookingSearchCriteria bookingSearchCriteria = CommunityHallBookingSearchCriteria.builder()
				.toDate(formattedDate).status(CHB_STATUS_BOOKED).build();
		List<CommunityHallBookingDetail> bookingDetails = bookingRepository.getBookingDetails(bookingSearchCriteria);

		if (bookingDetails == null || bookingDetails.isEmpty()) {
			return; // Exit if no booking details are found
		}

		UserDetailResponse userDetailResponse = userService.searchByUserName("9999009999", CHB_TENANTID);
		if (userDetailResponse == null || userDetailResponse.getUser().isEmpty()) {
			throw new IllegalStateException("SYSTEM user not found for tenant 'pg'.");
		}
		RequestInfo requestInfo = RequestInfo.builder().userInfo(userDetailResponse.getUser().get(0)).build();

		ProcessInstance workflow = ProcessInstance.builder().action(CHB_ACTION_MOVETOEMPLOYEE)
				.moduleName(CHB_REFUND_MODULENAME).tenantId(CHB_TENANTID).businessService(CHB_REFUND_BUSINESSSERVICE)
				.comment(null).documents(null).build();
		bookingDetails.forEach(bookingDetail -> {
			log.info("Updating Workflow of booking id ::::: " + bookingDetail.getBookingNo());
			bookingDetail.setWorkflow(workflow);
			CommunityHallBookingRequest bookingRequest = CommunityHallBookingRequest.builder()
					.hallsBookingApplication(bookingDetail).requestInfo(requestInfo) // Include the request info here
					.build();

			communityHallBookingService.updateBookingStatusWithWorkflow(bookingRequest);

		});
	}
	
}
