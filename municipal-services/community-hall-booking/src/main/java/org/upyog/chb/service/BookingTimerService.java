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
import org.upyog.chb.web.models.VenueSlotAvailabilityDetail;
import org.upyog.chb.web.models.VenueSlotSearchCriteria;

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
	 * Creates or returns the remaining payment timer when {@code isTimerRequired}
	 * is true on slot search.
	 *
	 * <p>
	 * If a final booking id is unavailable during slot search, a draft id is
	 * generated and used as the temporary timer booking reference. The create flow
	 * later replaces it with the actual booking id.
	 * </p>
	 *
	 * @param criteria                slot search criteria containing booking id or draft id
	 * @param info                    request metadata and authenticated user details
	 * @param availabilityDetailsList computed slot availability rows
	 * @return remaining timer value in seconds
	 */
	@Transactional
	public long managePaymentTimer(VenueSlotSearchCriteria criteria, RequestInfo info,
			List<VenueSlotAvailabilityDetail> availabilityDetailsList) {
		validateTimerCriteria(criteria, info);
		ensureTimerBookingReference(criteria);

		var existingTimers = bookingRepository.getBookingTimer(criteria);
		if (!CollectionUtils.isEmpty(existingTimers)) {
			log.info("Reusing existing payment timer for bookingId={}", getTimerBookingReference(criteria));
			return getTimerValue(existingTimers.get(0));
		}

		var userId = info.getUserInfo().getUuid();
		var hallCodes = CommunityHallSlotCriteriaUtil.resolveHallCodes(criteria);
		var bookingDates = CommunityHallSlotCriteriaUtil.resolveBookingDates(criteria);
		var createdTime = CommunityHallBookingUtil.getCurrentTimestamp();
		var activeTimersInRange = bookingRepository.getBookingTimerByCreatedBy(info, criteria);
		String fromTime = criteria.getFromTime();
		String toTime = criteria.getToTime();
		var timerDetails = new ArrayList<BookingPaymentTimerDetails>();
		var redis = paymentTimerRedis.getIfAvailable();

		for (var bookingDate : bookingDates) {
			for (var hallCode : hallCodes) {
				/*
				 * The DB and Redis use the same booking reference. For pre-create slot holds,
				 * this value is the draft id returned to the client.
				 */
				assertNoConflictingTimer(activeTimersInRange, criteria, userId, hallCode, bookingDate);

				var detail = PaymentTimerKeyBuilder.toTimerDetails(criteria.getTenantId(),
						criteria.getVenueCode(), hallCode, bookingDate, getTimerBookingReference(criteria), userId,
						createdTime);

				if (redis != null && !redis.tryAcquireSlot(detail,fromTime, toTime)) {
					throw new CustomException("SLOT_PAYMENT_TIMER_LOCKED",
							"Hall slot is held by another booking. hallCode=" + hallCode + " bookingDate=" + bookingDate);
				}
				timerDetails.add(detail);
			}
		}

		try {
			boolean isFinalBooking = criteria.getBookingId() != null && !criteria.getBookingId().isEmpty();
			bookingRepository.createBookingTimer(criteria, info, isFinalBooking, timerDetails);
			if (redis != null) {
				redis.syncTimerRows(timerDetails,fromTime , toTime);
			}
		} catch (RuntimeException ex) {
			if (redis != null) {
				redis.removeTimerRows(timerDetails,fromTime , toTime);
			}
			throw ex;
		}

		var timerValue = CommunityHallBookingUtil
				.getSeconds(Integer.parseInt(bookingConfiguration.getBookingPaymentTimerValue()));
		log.info("Payment timer created bookingId={} hallCodes={} timerValueSeconds={}", getTimerBookingReference(criteria),
				hallCodes, timerValue);
		return timerValue;
	}

	/**
	 * Delegates timer creation or reuse for callers that need a timer value from
	 * slot search.
	 *
	 * @param criteria                slot search criteria
	 * @param info                    request metadata
	 * @param availabilityDetailsList computed slot availability rows
	 * @return remaining timer value in seconds
	 */
	@Transactional
	public long getTimerValue(VenueSlotSearchCriteria criteria, RequestInfo info,
			List<VenueSlotAvailabilityDetail> availabilityDetailsList) {
		return managePaymentTimer(criteria, info, availabilityDetailsList);
	}

	/**
	 * Fetches the current remaining timer value for a created booking.
	 *
	 * @param bookingId final booking id
	 * @return remaining timer value in seconds, or {@code 0} when no timer exists
	 */
	public long getRemainingTimerValue(String bookingId) {
		if (StringUtils.isBlank(bookingId)) {
			return 0L;
		}
		var criteria = VenueSlotSearchCriteria.builder().bookingId(bookingId).build();
		var timers = bookingRepository.getBookingTimer(criteria);
		if (CollectionUtils.isEmpty(timers)) {
			return 0L;
		}
		return Math.max(getTimerValue(timers.get(0)), 0L);
	}

	/**
	 * Checks whether a requested hall/date pair is already held by another timer.
	 *
	 * @param activeTimersInRange active timer rows for the requested slot range
	 * @param criteria            current slot search criteria
	 * @param userId              authenticated user uuid
	 * @param hallCode            hall code being checked
	 * @param bookingDate         booking date being checked
	 */
	private void assertNoConflictingTimer(List<BookingPaymentTimerDetails> activeTimersInRange,
			VenueSlotSearchCriteria criteria, String userId, String hallCode,
			java.time.LocalDate bookingDate) {
		var conflict = activeTimersInRange.stream()
				.anyMatch(t -> hallCode.equals(t.getCode()) && bookingDate.equals(t.getBookingDate())
						&& !(userId.equals(t.getCreatedBy()) && getTimerBookingReference(criteria).equals(t.getBookingId())));
		if (conflict) {
			throw new CustomException("SLOT_PAYMENT_TIMER_LOCKED",
					"Hall slot already has an active payment timer. hallCode=" + hallCode + " bookingDate=" + bookingDate);
		}
	}

	/**
	 * Validates request data needed to create or reuse a payment timer.
	 *
	 * @param criteria slot search criteria
	 * @param info     request metadata
	 */
	private void validateTimerCriteria(VenueSlotSearchCriteria criteria, RequestInfo info) {
		if (info == null || info.getUserInfo() == null || StringUtils.isBlank(info.getUserInfo().getUuid())) {
			throw new CustomException(CommunityHallBookingConstants.INVALID_SEARCH,
					"Authenticated user is required for payment timer");
		}
		CommunityHallSlotCriteriaUtil.resolveHallCodes(criteria);
	}

	/**
	 * Ensures the timer has an identifier before inserting timer rows.
	 *
	 * @param criteria slot search criteria that may receive a generated draft id
	 */
	private void ensureTimerBookingReference(VenueSlotSearchCriteria criteria) {
		if (StringUtils.isBlank(criteria.getBookingId()) && StringUtils.isBlank(criteria.getDraftId())) {
			criteria.setDraftId(CommunityHallBookingUtil.getRandonUUID());
		}
	}

	/**
	 * Resolves the timer booking reference, preferring the final booking id when it
	 * exists and falling back to draft id during pre-create slot holds.
	 *
	 * @param criteria slot search criteria
	 * @return booking id or draft id used by timer rows
	 */
	private String getTimerBookingReference(VenueSlotSearchCriteria criteria) {
		return StringUtils.isNotBlank(criteria.getBookingId()) ? criteria.getBookingId() : criteria.getDraftId();
	}

	/**
	 * Calculates remaining timer seconds from a persisted timer row.
	 *
	 * @param bookingPaymentTimerDetails timer row from the database
	 * @return remaining timer value in seconds
	 */
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

	/**
	 * Deletes payment timer rows for a booking and clears their Redis mirror.
	 *
	 * @param bookingId           booking id used in timer rows
	 * @param updateBookingStatus whether the booking should be expired after deletion
	 */
	@Transactional
	public void deleteBookingTimer(String bookingId, boolean updateBookingStatus) {
		log.info("Deleting timer entry for booking id : {}", bookingId);
		removeRedisMirrorForBooking(bookingId);
		bookingRepository.deleteBookingTimer(bookingId, updateBookingStatus);
	}

	/**
	 * Removes Redis mirror rows for a booking timer.
	 *
	 * @param bookingId booking id used in timer rows
	 */
	private void removeRedisMirrorForBooking(String bookingId) {
		var redis = paymentTimerRedis.getIfAvailable();
		if (redis == null) {
			return;
		}
		var criteria = VenueSlotSearchCriteria.builder().bookingId(bookingId).build();
		var timers = bookingRepository.getBookingTimer(criteria);
		if (!CollectionUtils.isEmpty(timers)) {
			redis.removeTimerRows(timers,criteria.getFromTime(),criteria.getToTime());
		}
	}

	/**
	 * Fetches active timer rows that overlap the requested slot range.
	 *
	 * @param info     request metadata
	 * @param criteria slot search criteria
	 * @return matching timer rows
	 */
	public List<BookingPaymentTimerDetails> getBookingFromTimerTable(RequestInfo info,
			VenueSlotSearchCriteria criteria) {
		return bookingRepository.getBookingTimerByCreatedBy(info, criteria);
	}
}
