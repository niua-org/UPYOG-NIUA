package org.upyog.adv.service;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.upyog.adv.repository.BookingRepository;
import org.upyog.adv.util.BookingUtil;
import org.upyog.adv.util.PaymentTimerKeyBuilder;
import org.upyog.adv.web.models.AdvertisementSlotAvailabilityDetail;
import org.upyog.adv.web.models.AdvertisementSlotSearchCriteria;
import org.upyog.adv.web.models.BookingPaymentTimerDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Payment timer holds are stored in {@code eg_adv_payment_timer} ({@link BookingPaymentTimerDetails}).
 * Redis (optional) mirrors the same keys when {@code adv.payment.timer.redis.enabled=true}.
 */
@Service
@Slf4j
public class PaymentTimerService {

	@Autowired
	@Lazy
	private BookingRepository bookingRepository;

	@Autowired
	private ObjectProvider<PaymentTimerRedisService> paymentTimerRedis;

	@Transactional
	public void insertBookingIdForTimer(List<AdvertisementSlotSearchCriteria> criteriaList, RequestInfo requestInfo,
			List<AdvertisementSlotAvailabilityDetail> availabiltityDetailsResponse) {
		var uuid = requestInfo.getUserInfo().getUuid();
		var tenantId = criteriaList.stream().map(AdvertisementSlotSearchCriteria::getTenantId)
				.filter(StringUtils::isNotBlank).findFirst()
				.orElse(requestInfo.getUserInfo().getTenantId());
		var existingDraftId = bookingRepository.fetchDraftIdForTimer(criteriaList, uuid, tenantId);
		var willInsertNewTimer = existingDraftId == null;

		List<BookingPaymentTimerDetails> timerDetails = Collections.emptyList();
		String preGeneratedDraftId = null;
		var redis = paymentTimerRedis.getIfAvailable();

		if (willInsertNewTimer) {
			preGeneratedDraftId = BookingUtil.getRandonUUID();
			var createdTime = BookingUtil.getCurrentTimestamp();
			timerDetails = PaymentTimerKeyBuilder.buildTimerDetailsList(criteriaList, preGeneratedDraftId, uuid,
					tenantId, createdTime);

			if (redis != null) {
				for (var detail : timerDetails) {
					if (!redis.tryAcquireSlot(detail)) {
						throw new CustomException("SLOT_PAYMENT_TIMER_LOCKED",
								"Advertisement slot is held by another booking. addType=" + detail.getAddType()
										+ " location=" + detail.getLocation() + " bookingDate="
										+ detail.getBookingDate());
					}
				}
			}
		}

		try {
			bookingRepository.insertBookingIdForTimer(criteriaList, requestInfo,
					availabiltityDetailsResponse.get(0), preGeneratedDraftId);
			if (redis != null && !CollectionUtils.isEmpty(timerDetails)) {
				redis.syncTimerRows(timerDetails);
			}
		} catch (RuntimeException ex) {
			if (redis != null && !CollectionUtils.isEmpty(timerDetails)) {
				redis.removeTimerRows(timerDetails);
			}
			throw ex;
		}
	}

	@Transactional
	public void deleteBookingIdForTimer(String bookingId, RequestInfo requestInfo) {
		log.info("Deleting timer entry for booking id : {}", bookingId);
		removeRedisMirrorForBooking(bookingId);
		bookingRepository.deleteBookingIdForTimer(bookingId);
	}

	@Transactional
	public void deleteDataFromTimerAndDraft(String uuid, String draftId, String bookingId) {
		if (StringUtils.isBlank(draftId) && StringUtils.isBlank(bookingId)) {
			removeRedisMirrorForUser(uuid);
		}
		bookingRepository.deleteDataFromTimerAndDraft(uuid, draftId, bookingId);
	}

	@Transactional
	public void removeRedisMirrorForDraft(String draftId) {
		removeRedisMirrorForBooking(draftId);
	}

	private void removeRedisMirrorForBooking(String bookingId) {
		var redis = paymentTimerRedis.getIfAvailable();
		if (redis == null || StringUtils.isBlank(bookingId)) {
			return;
		}
		var timers = filterRedisEligibleTimers(bookingRepository.getPaymentTimerByBookingId(bookingId));
		if (!CollectionUtils.isEmpty(timers)) {
			redis.removeTimerRows(timers);
		}
	}

	private void removeRedisMirrorForUser(String uuid) {
		var redis = paymentTimerRedis.getIfAvailable();
		if (redis == null || StringUtils.isBlank(uuid)) {
			return;
		}
		var timers = filterRedisEligibleTimers(bookingRepository.getPaymentTimerByCreatedBy(uuid));
		if (!CollectionUtils.isEmpty(timers)) {
			redis.removeTimerRows(timers);
		}
	}

	private List<BookingPaymentTimerDetails> filterRedisEligibleTimers(List<BookingPaymentTimerDetails> timers) {
		return timers.stream()
				.filter(timer -> StringUtils.isNotBlank(timer.getTenantId()) && timer.getBookingDate() != null
						&& StringUtils.isNotBlank(timer.getAddType()) && StringUtils.isNotBlank(timer.getLocation())
						&& StringUtils.isNotBlank(timer.getFaceArea()))
				.toList();
	}
}
