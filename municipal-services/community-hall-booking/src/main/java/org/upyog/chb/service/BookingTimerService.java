package org.upyog.chb.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.constants.CommunityHallBookingConstants;
import org.upyog.chb.repository.CommunityHallBookingRepository;
import org.upyog.chb.util.CommunityHallBookingUtil;
import org.upyog.chb.util.CommunityHallSlotCriteriaUtil;
import org.upyog.chb.util.PaymentTimerKeyBuilder;
import org.upyog.chb.web.models.BookingPaymentTimerDetails;
import org.upyog.chb.web.models.CommunityHallSlotAvailabilityDetail;
import org.upyog.chb.web.models.CommunityHallSlotSearchCriteria;

import lombok.extern.slf4j.Slf4j;

/**
 * Payment timer holds are stored in {@code eg_chb_payment_timer} ({@link BookingPaymentTimerDetails}).
 * Redis (optional) mirrors the same keys — no {@code seat_locks} table.
 */
@Service
@Slf4j
public class BookingTimerService {

	@Autowired
	private CommunityHallBookingRepository bookingRepository;

	@Autowired
	private CommunityHallBookingConfiguration bookingConfiguration;

	@Autowired
	private ObjectProvider<PaymentTimerRedisService> paymentTimerRedis;

	/**
	 * Creates or returns remaining payment timer when {@code isTimerRequired} is true on slot search.
	 */
	@Transactional
	public long managePaymentTimer(CommunityHallSlotSearchCriteria criteria, RequestInfo info,
			List<CommunityHallSlotAvailabilityDetail> availabilityDetailsList) {
		validateTimerCriteria(criteria, info);

		var existingTimers = bookingRepository.getBookingTimer(criteria);
		if (!CollectionUtils.isEmpty(existingTimers)) {
			log.info("Reusing existing payment timer for bookingId={}", criteria.getBookingId());
			return getTimerValue(existingTimers.get(0));
		}

		var userId = info.getUserInfo().getUuid();
		var hallCodes = CommunityHallSlotCriteriaUtil.resolveHallCodes(criteria);
		var bookingDates = CommunityHallSlotCriteriaUtil.resolveBookingDates(criteria);
		var createdTime = CommunityHallBookingUtil.getCurrentTimestamp();
		var activeTimersInRange = bookingRepository.getBookingTimerByCreatedBy(info, criteria);

		var timerDetails = new ArrayList<BookingPaymentTimerDetails>();
		var redis = paymentTimerRedis.getIfAvailable();

		for (var bookingDate : bookingDates) {
			for (var hallCode : hallCodes) {
				assertNoConflictingTimer(activeTimersInRange, criteria, userId, hallCode, bookingDate);

				var detail = PaymentTimerKeyBuilder.toTimerDetails(criteria.getTenantId(),
						criteria.getCommunityHallCode(), hallCode, bookingDate, criteria.getBookingId(), userId,
						createdTime);

				if (redis != null && !redis.tryAcquireSlot(detail)) {
					throw new CustomException("SLOT_PAYMENT_TIMER_LOCKED",
							"Hall slot is held by another booking. hallCode=" + hallCode + " bookingDate=" + bookingDate);
				}
				timerDetails.add(detail);
			}
		}

		try {
			bookingRepository.createBookingTimer(criteria, info, true, timerDetails);
			if (redis != null) {
				redis.syncTimerRows(timerDetails);
			}
		} catch (RuntimeException ex) {
			if (redis != null) {
				redis.removeTimerRows(timerDetails);
			}
			throw ex;
		}

		var timerValue = CommunityHallBookingUtil
				.getSeconds(Integer.parseInt(bookingConfiguration.getBookingPaymentTimerValue()));
		log.info("Payment timer created bookingId={} hallCodes={} timerValueSeconds={}", criteria.getBookingId(),
				hallCodes, timerValue);
		return timerValue;
	}

	@Transactional
	public long getTimerValue(CommunityHallSlotSearchCriteria criteria, RequestInfo info,
			List<CommunityHallSlotAvailabilityDetail> availabilityDetailsList) {
		return managePaymentTimer(criteria, info, availabilityDetailsList);
	}

	private void assertNoConflictingTimer(List<BookingPaymentTimerDetails> activeTimersInRange,
			CommunityHallSlotSearchCriteria criteria, String userId, String hallCode,
			java.time.LocalDate bookingDate) {
		var conflict = activeTimersInRange.stream()
				.anyMatch(t -> hallCode.equals(t.getHallcode()) && bookingDate.equals(t.getBookingDate())
						&& !(userId.equals(t.getCreatedBy()) && criteria.getBookingId().equals(t.getBookingId())));
		if (conflict) {
			throw new CustomException("SLOT_PAYMENT_TIMER_LOCKED",
					"Hall slot already has an active payment timer. hallCode=" + hallCode + " bookingDate=" + bookingDate);
		}
	}

	private void validateTimerCriteria(CommunityHallSlotSearchCriteria criteria, RequestInfo info) {
		if (StringUtils.isBlank(criteria.getBookingId())) {
			throw new CustomException("INVALID_BOOKING_ID", "bookingId is required when payment timer is enabled");
		}
		if (info == null || info.getUserInfo() == null || StringUtils.isBlank(info.getUserInfo().getUuid())) {
			throw new CustomException(CommunityHallBookingConstants.INVALID_SEARCH,
					"Authenticated user is required for payment timer");
		}
		CommunityHallSlotCriteriaUtil.resolveHallCodes(criteria);
	}

	private Long getTimerValue(BookingPaymentTimerDetails bookingPaymentTimerDetails) {
		long timerValue = CommunityHallBookingUtil
				.getSeconds(Integer.parseInt(bookingConfiguration.getBookingPaymentTimerValue()));
		long currentTimestamp = CommunityHallBookingUtil.getCurrentTimestamp();
		long timeSpentAfterCreation = CommunityHallBookingUtil.calculateDifferenceInSeconds(currentTimestamp,
				bookingPaymentTimerDetails.getCreatedTime());
		log.info("currentTimestamp : {} createdTime : {} timeSpentAfterCreation : {} ", currentTimestamp,
				bookingPaymentTimerDetails.getCreatedTime(), timeSpentAfterCreation);
		return timerValue - timeSpentAfterCreation;
	}

	@Transactional
	public void deleteBookingTimer(String bookingId, boolean updateBookingStatus) {
		log.info("Deleting timer entry for booking id : {}", bookingId);
		removeRedisMirrorForBooking(bookingId);
		bookingRepository.deleteBookingTimer(bookingId, updateBookingStatus);
	}

	private void removeRedisMirrorForBooking(String bookingId) {
		var redis = paymentTimerRedis.getIfAvailable();
		if (redis == null) {
			return;
		}
		var criteria = CommunityHallSlotSearchCriteria.builder().bookingId(bookingId).build();
		var timers = bookingRepository.getBookingTimer(criteria);
		if (!CollectionUtils.isEmpty(timers)) {
			redis.removeTimerRows(timers);
		}
	}

	public List<BookingPaymentTimerDetails> getBookingFromTimerTable(RequestInfo info,
			CommunityHallSlotSearchCriteria criteria) {
		return bookingRepository.getBookingTimerByCreatedBy(info, criteria);
	}
}
